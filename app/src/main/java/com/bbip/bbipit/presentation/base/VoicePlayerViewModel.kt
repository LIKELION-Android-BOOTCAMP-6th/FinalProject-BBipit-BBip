package com.bbip.bbipit.presentation.base

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.util.AudioPlayer
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신 음성 메시지 UI 상태 관리 데이터 클래스
 */
data class IncomingVoiceUiState(
    val isVisible: Boolean = false,
    val sender: User? = null,
    val currentVoiceMessage: VoiceMessage? = null,
    val currentPosition: Int = 0
)

/**
 * 전역 음성 메시지 수신 및 자동 재생 관리용 ViewModel 클래스
 */
@HiltViewModel
class VoicePlayerViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : BaseViewModel<IncomingVoiceUiState>(IncomingVoiceUiState()) {

    private val audioPlayer = AudioPlayer(context)

    private val TAG = "VoicePlayerViewModel"

    init {
        listenToServiceVoiceEvent()
    }

    /**
     * 음성 메시지 수신 이벤트 구독 및 재생 처리 함수
     */
    private fun listenToServiceVoiceEvent() {
        viewModelScope.launch {
            // 음성 메시지 이벤트 구독
            voiceRepository.voiceMessageEvent.collect { voiceMessage ->
                val url = voiceMessage.voiceUrl

                // 발신자 프로필 조회
                val senderResult = userRepository.getUserProfile(voiceMessage.senderId)
                val sender = when (senderResult) {
                    is Result.Success -> senderResult.data
                    is Result.Failure -> null
                }

                // UI 노출 및 데이터 업데이트
                updateState {
                    copy(
                        isVisible = true,
                        sender = sender,
                        currentVoiceMessage = voiceMessage
                    )
                }

                // 오디오 재생 및 완료 처리
                audioPlayer.playFromUrl(url) {
                    viewModelScope.launch {
                        // 완료 시 재생 위치를 총 길이로 보정
                        updateState { copy(currentPosition = currentVoiceMessage?.duration ?: 0) }

                        // 음성 메시지 읽음 처리
                        voiceRepository.markVoiceMessageAsRead(voiceMessage.id)
                        delay(1000)
                        dismissMessage()
                    }
                }

                // 재생 진행 위치 추적 시작
                startPositionTracking()
            }
        }
    }

    /**
     * 수신 메시지 UI 및 데이터 초기화 함수
     */
    fun dismissMessage() {
        updateState { copy(isVisible = false, sender = null, currentVoiceMessage = null, currentPosition = 0) }
    }

    /**
     * 오디오 재생 진행 위치 추적 함수
     */
    private fun startPositionTracking() {
        viewModelScope.launch {
            var waitCount = 0
            val maxWaitAttempts = 25 // 최대 5초 대기 가드레일

            // 플레이어 준비 대기
            while (isActive && !audioPlayer.isPlaying() && waitCount < maxWaitAttempts) {
                delay(200)
                waitCount++
            }

            Log.d(TAG, "🎵 재생 감지 성공 (버퍼링 대기: ${waitCount * 200}ms) - 타이머 시작")

            // 재생 중인 동안 실시간으로 진행 시간 업데이트
            while (isActive && audioPlayer.isPlaying()) {
                val posSeconds = (audioPlayer.getCurrentPosition() / 1000)
                updateState { copy(currentPosition = posSeconds) }
                delay(200)
            }

            Log.d(TAG, "🔇 재생이 정지되었거나 종료되어 타이머를 마칩니다.")
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