package com.bbip.bbipit.core.base

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val userRepository: UserRepository
): DefaultLifecycleObserver {
    // 로그 출력용 클래스 식별 태그
    private val TAG = "LifeCycleManager"

    // 파이어베이스 사용자 인증 관리 인스턴스
    private val auth = FirebaseAuth.getInstance()

    // 메인 루퍼 기반 주기적 작업 예약용 핸들러
    private val handler = Handler(Looper.getMainLooper())

    // 하트비트 전송 작업 반복 수행용 Runnable 객체
    private var heartbeatRunnable: Runnable? = null

    // 데이터베이스 작업 비동기 처리용 IO 스레드 스코프
    private val sessionScope = CoroutineScope(Dispatchers.IO)

    // 하트비트 신호 전송 주기 간격 (30초)
    private val HEARTBEAT_INTERVAL = 30000L

    // 현재 진입한 채팅방 고유 식별자
    private var currentRoomId: String? = null

    // 앱의 현재 포어그라운드 위치 여부 플래그
    var isAppInForeground: Boolean = false
        private set

    var onAppForegroundStatusChanged: ((Boolean) -> Unit)? = null

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
        // 활성화 상태 플래그 변경
        isAppInForeground = true

        onAppForegroundStatusChanged?.invoke(true)
    }

    /**
     * 앱의 백그라운드 전환 콜백 함수
     */
    override fun onStop(owner: LifecycleOwner) {
        // 활성화 상태 플래그 변경
        isAppInForeground = false

        onAppForegroundStatusChanged?.invoke(true)
    }

    /**
     * 라이브 세션 가동 및 주기적 하트비트 시작 함수
     */
    fun startSession() {
        // 미인증 사용자의 접근 차단
        if (auth.currentUser == null) return
        Log.d(TAG, "🟢 전역 라이브 세션 가동 (하트비트 루프 시작)")

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
    }

    /**
     * 라이브 세션 중단 및 하트비트 루프 종료 함수
     */
    fun stopSession() {
        Log.d(TAG, "🔴 전역 라이브 세션 중단 (하트비트 중단)")

        // 진행 중인 하트비트 루프 중단
        stopHeartbeatLoop()

        // 백그라운드 진입 시 즉시 오프라인 상태 반영
        if (auth.currentUser != null) {
            sessionScope.launch {
                // 기존 라이프사이클 종료 신호 전송 (전입 방 정보 해제)
                liveStatusRepository.updateLifeCycle(null)

                // 오프라인 상태 서버에 즉시 반영
                userRepository.updateOnlineStatus(false)
            }
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
}