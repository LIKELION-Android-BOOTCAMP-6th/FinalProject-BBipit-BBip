package com.bbip.bbipit.presentation.base

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.util.AudioPlayer
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.bbip.bbipit.core.result.Result

/**
 * 수신 음성 메시지 UI 상태 관리 데이터 클래스
 */
data class IncomingVoiceUiState(
    val isVisible: Boolean = false,
    val senderName: String = "",
    val senderProfileUrl: String = "",
    val currentVoiceMessage: VoiceMessage? = null,
    val currentPosition: Int = 0
)

/**
 * 전역 음성 메시지 수신 및 자동 재생 관리용 ViewModel 클래스
 */
@HiltViewModel
class VoicePlayerViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository,
    @ApplicationContext private val context: Context
) : BaseViewModel<IncomingVoiceUiState>(IncomingVoiceUiState()) {

    private val audioPlayer = AudioPlayer(context)

    private val TAG = "VoicePlayerViewModel"

    init {
        listenToServiceVoiceEvent()
    }

    /**
     * 특정 음성 메시지 ID를 받아 서버에서 데이터를 조회한 후
     * 즉시 재생 및 UI 상태를 처리하는 함수
     */
    fun playVoiceMessage(messageId: String) {
        viewModelScope.launch {
            // 서버에서 음성 메시지 조회
            val result = voiceRepository.getVoiceMessageById(messageId)

            when (result) {
                is Result.Success -> {
                    val voiceMessage = result.data
                    val url = voiceMessage.voiceUrl

                    updateState {
                        copy(
                            isVisible = true,
                            senderName = voiceMessage.senderName,
                            senderProfileUrl = voiceMessage.senderProfileUrl,
                            currentVoiceMessage = voiceMessage
                        )
                    }

                    // 3. 오디오 재생 및 완료 콜백 처리
                    audioPlayer.playFromUrl(url) {
                        viewModelScope.launch {
                            // 완료 시 재생 위치를 총 길이로 보정
                            updateState { copy(currentPosition = currentVoiceMessage?.duration ?: 0) }

                            // 음성 메시지 읽음 처리 (기존 로직 유지)
                            voiceRepository.markVoiceMessageAsRead(voiceMessage.id)
                            delay(1000)
                            dismissMessage()
                        }
                    }

                    // 4. 재생 진행 위치 추적 시작
                    startPositionTracking()
                }
                is Result.Failure -> {
                    // 필요 시 에러 토스트 팝업이나 로그 처리 추가 가능
                    Log.e(TAG, "음성 메시지 재생 실패: ${result.error}")
                }
            }
        }
    }

    /**
     * 음성 메시지 수신 이벤트 구독 및 재생 처리 함수
     */
    private fun listenToServiceVoiceEvent() {
        viewModelScope.launch {
            // 음성 메시지 이벤트 구독
            voiceRepository.voiceMessageEvent.collect { voiceMessage ->
                val url = voiceMessage.voiceUrl

                // 이전 재생 버퍼나 꼬인 세션이 있다면 강제 정지 및 초기화 수행
                runCatching { audioPlayer.stopAudio() }

                // UI 노출 및 데이터 업데이트
                updateState {
                    copy(
                        isVisible = true,
                        senderName = voiceMessage.senderName,
                        senderProfileUrl = voiceMessage.senderProfileUrl,
                        currentVoiceMessage = voiceMessage
                    )
                }

                // 오디오 재생 및 완료 처리
                try {
                    audioPlayer.playFromUrl(url) {
                        viewModelScope.launch {
                            // 완료 시 재생 위치를 총 길이로 보정
                            updateState {
                                copy(
                                    currentPosition = currentVoiceMessage?.duration ?: 0
                                )
                            }

                            // 음성 메시지 읽음 처리
                            voiceRepository.markVoiceMessageAsRead(voiceMessage.id)
                            delay(1000)
                            dismissMessage()
                        }
                    }
                    // 재생 성공 시진행 추적 타이머 가동
                    startPositionTracking()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 재생 중 예외 발생하여 화면을 닫습니다: ${e.message}")
                    dismissMessage()
                }
            }
        }
    }

    /**
     * 수신 메시지 UI 및 데이터 초기화 함수
     */
    fun dismissMessage() {
        updateState { copy(isVisible = false, senderName = "", senderProfileUrl = "", currentVoiceMessage = null, currentPosition = 0) }
    }

    /**
     * 오디오 재생 진행 위치 추적 함수
     */
    private fun startPositionTracking() {
        viewModelScope.launch {
            var waitCount = 0
            val maxWaitAttempts = 15 // 최대 3초 대기 가드레일

            // 플레이어 준비 대기
            while (isActive && !audioPlayer.isPlaying() && waitCount < maxWaitAttempts) {
                delay(200)
                waitCount++
            }

            // 만약 대기 시간이 지났는데도 플레이어가 실행되지 않는다면 상태가 깨진 것으로 판단
            if (waitCount >= maxWaitAttempts && !audioPlayer.isPlaying()) {
                Log.w(TAG, "⚠️ 오디오 플레이어 버퍼 로딩 실패 가드레일 작동 -> 세션 종료")
                dismissMessage()
                return@launch
            }

            Log.d(TAG, "🎵 재생 감지 성공 - 타이머 시작")

            // 재생 중인 동안 실시간으로 진행 시간 업데이트
            while (isActive && audioPlayer.isPlaying()) {
                val posSeconds = (audioPlayer.getCurrentPosition() / 1000)
                updateState { copy(currentPosition = posSeconds) }
                delay(200)
            }

            Log.d(TAG, "🔇 재생이 정지되어 타이머를 마칩니다.")
        }
    }

    /**
     * 뷰모델 소멸 시 플레이어 자원 해제 함수
     */
    override fun onCleared() {
        super.onCleared()
        audioPlayer.stopAudio()
    }
}