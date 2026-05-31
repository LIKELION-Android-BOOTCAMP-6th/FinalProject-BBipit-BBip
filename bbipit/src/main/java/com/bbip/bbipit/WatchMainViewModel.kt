package com.bbip.bbipit

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.models.MobileServiceStatus
import com.bbip.bbipit.models.WatchVoiceData
import com.bbip.bbipit.service.WatchCentralService
import com.bbip.bbipit.util.VoiceEventBus
import com.bbip.bbipit.util.WatchAudioPlayer
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 메인 화면 UI 상태 관리 뷰모델
 */
class WatchMainViewModel (application: Application) : AndroidViewModel(application) {

    val TAG = "WatchMainViewModel"

    // 음성 메시지 수신 상태 플로우
    private val _voiceUiState = MutableStateFlow<WatchVoiceData?>(null)

    // 음성 수신 상태 스트림
    val voiceUiState = _voiceUiState.asStateFlow()

    private val audioPlayer = WatchAudioPlayer.getInstance(application)

    // 휴대폰 백그라운드 서비스 상태를 관리하는 StateFlow (기본값은 확인 중)
    private val _mobileStatus = MutableStateFlow(MobileServiceStatus.CHECKING)
    val mobileStatus: StateFlow<MobileServiceStatus> = _mobileStatus.asStateFlow()

    private val messageClient = Wearable.getMessageClient(application)
    private val nodeClient = Wearable.getNodeClient(application)

    init {
        // 음성 수신 이벤트 구독 및 상태 업데이트
        viewModelScope.launch {
            VoiceEventBus.incomingVoiceEvent.collect { data ->
                if(data != null) {
                    _voiceUiState.value = data
                    playVoice(data)
                }
            }
        }
    }

    private fun playVoice(voiceData: WatchVoiceData) {
        audioPlayer.playFromUrl(voiceData.voiceUrl) {
            Log.d("WatchMainViewModel", "🎵 오디오 재생 완료: ${voiceData.messageId}")

            // UI 닫기
            _voiceUiState.value = null

            // 스마트폰으로 읽음 완료 패킷 전송
            viewModelScope.launch(Dispatchers.IO) {
                sendReadStatusToPhone(voiceData.messageId)
            }
        }
    }

    private suspend fun sendReadStatusToPhone(messageId: String) {
        try {
            val context = getApplication<Application>()
            val nodeClient = Wearable.getNodeClient(context)
            val messageClient = Wearable.getMessageClient(context)
            val nodes = nodeClient.connectedNodes.await()
            val phoneNode = nodes.firstOrNull()

            phoneNode?.let {
                messageClient.sendMessage(
                    it.id,
                    "/mark_voice_read",
                    messageId.toByteArray(Charsets.UTF_8)
                ).await()
                Log.d("WatchMainViewModel", "✅ 스마트폰으로 읽음 신호 전송 완료")
            }
        } catch (e: Exception) {
            Log.e("WatchMainViewModel", "❌ 읽음 신호 전송 실패", e)
        }
    }

    /**
     * 휴대폰 백그라운드 서비스 상태 확인 요청 및 타임아웃 검증
     */
    fun checkPhoneServiceStatus() {
        viewModelScope.launch {
            _mobileStatus.value = MobileServiceStatus.CHECKING

            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.e(TAG, "❌ 연결된 휴대폰 디바이스가 없습니다.")
                    _mobileStatus.value = MobileServiceStatus.SERVICE_RESTRICTED
                    return@launch
                }

                // 1. 휴대폰으로 상태 체크 신호 송신
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/check_phone_status", byteArrayOf()).await()
                }
                Log.d(TAG, "📲 휴대폰으로 상태 신호 송신 후 응답 대기 시작 (타임아웃 3초)")

                // 2. 💡 [핵심 예외 방어] 코루틴 타임아웃 지정 (3000ms = 3초)
                // 3초 내에 WatchCentralService의 이벤트 버스로 신호가 오는지 대기합니다.
                val resultStatus = withTimeoutOrNull(3000L) {
                    WatchCentralService.mobileStatusEventBus.first()
                }

                // 3. 결과에 따른 상태 제어
                if (resultStatus == null) {
                    // 3초 동안 휴대폰 서비스로부터 아무런 응답이 오지 않은 경우 (프로세스 Dead 상태 등)
                    Log.w(TAG, "⏳ 휴대폰 응답 타임아웃 초과! 서비스를 제한합니다.")
                    _mobileStatus.value = MobileServiceStatus.SERVICE_RESTRICTED
                } else {
                    // 성공적으로 응답을 수신한 경우
                    Log.d(TAG, "📱 최종 수신된 휴대폰 상태 상태: $resultStatus")
                    if (resultStatus == "READY") {
                        _mobileStatus.value = MobileServiceStatus.READY
                    } else {
                        _mobileStatus.value = MobileServiceStatus.SERVICE_RESTRICTED
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 상태 체크 프로세스 중 에러 발생", e)
                _mobileStatus.value = MobileServiceStatus.SERVICE_RESTRICTED
            }
        }
    }

    /**
     * 음성 데이터 상태 초기화
     */
    fun clearVoiceState() {
        _voiceUiState.value = null
    }
}