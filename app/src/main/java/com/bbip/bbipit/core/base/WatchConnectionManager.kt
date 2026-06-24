package com.bbip.bbipit.core.base

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 워치와의 연결 상태(물리적 연결 + 워치 앱 포그라운드 상태)를 총괄 관리하는 매니저
 */
@javax.inject.Singleton
class WatchConnectionManager @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : CapabilityClient.OnCapabilityChangedListener, MessageClient.OnMessageReceivedListener{

    private val TAG = "WatchConnectionManager"

    private val managerJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + managerJob)

    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val _isPhysicalConnected = MutableStateFlow(false)
    val isPhysicalConnected: StateFlow<Boolean> = _isPhysicalConnected.asStateFlow()

    private val _isWatchInForeground = MutableStateFlow(false)
    val isWatchInForeground: StateFlow<Boolean> = _isWatchInForeground.asStateFlow()

    companion object {
        private const val WATCH_CAPABILITY = "WATCH_APP_CONNECTED"
        private const val PATH_WATCH_STATE = "/watch_state"
    }

    /**
     * 감지 모니터링 가동
     */
    fun startMonitoring() {
        Log.d(TAG, "🔄 워치 종합 상태 모니터링 시작")
        capabilityClient.addListener(this, WATCH_CAPABILITY)
        messageClient.addListener(this) // Message 리스너 추가 등록

        // 초기 물리적 연결 상태 확인
        checkCurrentPhysicalConnection()
    }

    /**
     * 감지 모니터링 중단
     */
    fun stopMonitoring() {
        Log.d(TAG, "🛑 워치 종합 상태 모니터링 중단")
        capabilityClient.removeListener(this)
        messageClient.removeListener(this)
    }

    /**
     * 물리적 네트워크망 변경 감지
     */
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        checkCurrentPhysicalConnection()
    }

    /**
     * 워치 앱 상태 패킷 수신
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH_WATCH_STATE) {
            val state = String(messageEvent.data).toBoolean()
            _isWatchInForeground.value = state
            Log.d(TAG, "⌚ [매니저 수신] 워치 앱 상태 변경 감지 -> 포어그라운드 여부: $state")
        }
    }

    /**
     * 근거리 기기 실재 여부 체크
     */
    private fun checkCurrentPhysicalConnection() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val isConnected = nodes.any { it.isNearby }
                _isPhysicalConnected.value = isConnected

                // 만약 물리적 연결이 끊기면 워치 앱 포그라운드 상태도 강제로 false 처리 (방어 코드)
                if (!isConnected) {
                    _isWatchInForeground.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 워치 노드 조회 실패", e)
                _isPhysicalConnected.value = false
                _isWatchInForeground.value = false
            }
        }
    }
}