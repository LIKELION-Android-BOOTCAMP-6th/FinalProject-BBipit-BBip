package com.bbip.bbipit.core.base

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 실시간 접속 상태 및 하트비트(Heartbeat) 신호 전송 관리 클래스
 * 주기적인 서버 활성 상태 동기화 및 현재 활성화된 채팅방 정보 추적 수행
 */
@Singleton
class LifeCycleManager @Inject constructor(
    private val liveStatusRepository: LiveStatusRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context // [추가] 네트워크 매니저 접근을 위한 컨텍스트
) : DefaultLifecycleObserver {

    private val TAG = "LifeCycleManager"

    // 파이어베이스 사용자 인증 관리 인스턴스
    private val auth = FirebaseAuth.getInstance()

    // 메인 루퍼 기반 주기적 작업 예약용 핸들러
    private val handler = Handler(Looper.getMainLooper())

    // 하트비트 전송 작업 반복 수행용 Runnable 객체
    private var heartbeatRunnable: Runnable? = null

    // 데이터베이스 작업 비동기 처리용 IO 스레드 스코프
    private val sessionScope = CoroutineScope(Dispatchers.IO)

    // 하트비트 신호 전송 주기 간격 (60초)
    private val HEARTBEAT_INTERVAL = 60000L

    // 현재 진입한 채팅방 고유 식별자
    private var currentRoomId: String? = null

    // 앱의 현재 포어그라운드 위치 여부 플래그
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    var onAppForegroundStatusChanged: ((Boolean) -> Unit)? = null

    // 네트워크 콜백
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // 데이터베이스 인스턴스 초기화
    private val rtdb =
        FirebaseDatabase.getInstance("https://bbipit-default-rtdb.asia-southeast1.firebasedatabase.app/")

    // 서버 타임스탬프 밸류 매핑 예시
    val timestamp = ServerValue.TIMESTAMP
    private val _isNetworkConnected = MutableStateFlow(true)
    val isNetworkConnected: StateFlow<Boolean> = _isNetworkConnected.asStateFlow()

    /**
     * 현재 활성화된 채팅방 정보 갱신 및 상태 전송 함수
     */
    fun updateCurrentRoom(roomId: String?) {
        // 현재 활성화된 채팅방 정보 갱신
        this.currentRoomId = roomId

        // 변경된 상태 서버에 즉시 전송
        triggerHeartbeat()
    }

    /**
     * 앱의 포어그라운드 전환 콜백 함수
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "🏢 앱 포그라운드 진입 (ON_START)")
        _isAppInForeground.value = true // 플래그 업데이트

        onAppForegroundStatusChanged?.invoke(true)

        startSession()
        registerNetworkCallback() // 네트워크 감지 시작
    }

    /**
     * 앱의 백그라운드 전환 콜백 함수
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "🏠 앱 백그라운드 진입 (ON_STOP)")
        _isAppInForeground.value = false // 플래그 업데이트

        onAppForegroundStatusChanged?.invoke(false)

        stopSession()
        unregisterNetworkCallback() // 네트워크 감지 해제
    }

    /**
     * 라이브 세션 가동 및 주기적 하트비트 시작 함수
     */
    fun startSession() {
        // 미인증 사용자의 접근 차단
        val currentUser = auth.currentUser ?: return
        Log.d(TAG, "🟢 전역 라이브 세션 가동 (RTDB 소켓 연결 확보)")

        // 기존 진행 중인 하트비트 종료
        stopHeartbeatLoop()

        // 30초 주기로 하트비트를 반복 전송하는 작업 정의
        heartbeatRunnable = object : Runnable {
            override fun run() {
                triggerHeartbeat()
                handler.postDelayed(this, HEARTBEAT_INTERVAL)
            }
        }

        // 하트비트 반복 실행 예약
        handler.post(heartbeatRunnable!!)

        rtdb.purgeOutstandingWrites()

        // RTDB 연결 상태 모니터링 및 onDisconnect 예약
        val userStatusRef = rtdb.getReference("/status/${currentUser.uid}")

        // 커넥션이 수립되면 서버에 상태 기록
        val onlineStatus = mapOf(
            "state" to "online",
            "last_changed" to ServerValue.TIMESTAMP
        )
        val offlineStatus = mapOf(
            "state" to "offline",
            "last_changed" to ServerValue.TIMESTAMP
        )

        // 연결이 끊어졌을 때 서버 측에서 처리할 오프라인 액션을 미리 예약
        userStatusRef.onDisconnect().setValue(offlineStatus).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 예약 성공 후 현재 상태를 online으로 변경
                userStatusRef.setValue(onlineStatus)
            }
        }

        rtdb.goOnline()
    }

    /**
     * 라이브 세션 중단 및 하트비트 루프 종료 함수
     */
    fun stopSession() {
        Log.d(TAG, "🔴 전역 라이브 세션 중단 (하트비트 중단)")
        val currentUser = auth.currentUser ?: return

        // 진행 중인 하트비트 루프 중단
        stopHeartbeatLoop()

        // 명시적으로 나갈 때는 온디스커넥트를 해제하고 직접 offline을 박아줍니다.
        val userStatusRef = rtdb.getReference("/status/${currentUser.uid}")
        userStatusRef.onDisconnect().cancel()
        userStatusRef.setValue(mapOf("state" to "offline", "last_changed" to ServerValue.TIMESTAMP))

        sessionScope.launch {
            liveStatusRepository.updateLifeCycle(null, null)
        }
    }

    /**
     * 현재 채팅방 위치 및 활성 상태 서버 전송 함수
     */
    private fun triggerHeartbeat() {
        // 로그아웃 상태일 경우 전송 취소
        if (auth.currentUser == null) return

        // 현재 채팅방 위치 및 활성 상태를 서버에 전송
        sessionScope.launch {
            liveStatusRepository.updateLifeCycle(currentRoomId)
        }
    }

    /**
     * 예약된 하트비트 작업 취소 및 자원 해제 함수
     */
    private fun stopHeartbeatLoop() {
        // 예약된 반복 작업 취소 및 메모리 초기화
        heartbeatRunnable?.let {
            handler.removeCallbacks(it)
            heartbeatRunnable = null
        }
    }

    /**
     * 실시간 네트워크 연결 감지 등록 함수
     */
    private fun registerNetworkCallback() {
        if (networkCallback != null) return // 이미 등록되어 있다면 스킵

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // 인터넷 연결 가능 여부 확인
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            // 네트워크가 다시 연결되었을 때 호출됨
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                sessionScope.launch(Dispatchers.Main.immediate) {
                    _isNetworkConnected.value = true
                }
                Log.d(TAG, "🌐 네트워크 재연결 감지!")

                // 포그라운드 상태이고 로그인된 유저가 있다면 즉시 온라인 업데이트
                if (_isAppInForeground.value && auth.currentUser != null) {
                    Log.d(TAG, "⚡ 포그라운드 상태 확인됨 -> 사용자를 즉시 온라인 상태로 전환합니다.")
                    sessionScope.launch {
//                        userRepository.updateOnlineStatus(true)
                    }
                    startSession()
                }
            }

            // 네트워크가 끊겼을 때 호출됨
            override fun onLost(network: Network) {
                super.onLost(network)

                sessionScope.launch(Dispatchers.Main.immediate) {
                    _isNetworkConnected.value = false
                }
                Log.w(TAG, "⚠️ 네트워크 연결 해제됨")

                rtdb.goOffline()
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    /**
     * 네트워크 콜백 해제 함수
     */
    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.e(TAG, "네트워크 콜백 해제 실패: ${e.message}")
            }
            networkCallback = null
        }
    }
}