package com.bbip.bbipit.domain.usecase

import com.bbip.bbipit.core.util.AudioPlayer
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.repository.VoiceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * 재생 상태를 나타내는 도메인 State
 */
sealed interface VoicePlaybackState {
    data class Started(val voiceMessage: VoiceMessage) : VoicePlaybackState
    data class Playing(val currentPosition: Int) : VoicePlaybackState
    object Completed : VoicePlaybackState
    object Error : VoicePlaybackState
}

/**
 * 전역 음성 메시지를 수신하여 자동 재생하고 진행 상태 및 읽음 처리를 관장하는 유즈케이스
 */
class ListenAndPlayVoiceUseCase @Inject constructor(
    private val voiceRepository: VoiceRepository
) {
    // 플레이어 제어 로직을 UseCase 내부(혹은 별도 PlayerManager)로 은닉
    private val audioPlayer = AudioPlayer()

    operator fun invoke(): Flow<VoicePlaybackState> = flow {
        voiceRepository.voiceMessageEvent.collect { voiceMessage ->
            // 이전 재생 세션 강제 정지
            runCatching { audioPlayer.stopAudio() }

            // 재생 시작 알림 (UI에 메시지 정보 표시용)
            emit(VoicePlaybackState.Started(voiceMessage))

            try {
                var isPlaybackFinished = false

                // 오디오 재생 시동
                audioPlayer.playFromUrl(voiceMessage.voiceUrl) {
                    isPlaybackFinished = true
                }

                // 가드레일 및 재생 위치 추적 타이머 실행
                if (waitForPlayerReady()) {
                    while (coroutineContext.isActive && audioPlayer.isPlaying()) {
                        val posSeconds = (audioPlayer.getCurrentPosition() / 1000)
                        emit(VoicePlaybackState.Playing(posSeconds))
                        delay(200)
                    }
                } else {
                    // 버퍼 로딩 실패 시 에러 처리
                    emit(VoicePlaybackState.Error)
                    return@collect
                }

                // 완료 처리 및 비즈니스 정책 수행 (읽음 처리)
                if (isPlaybackFinished || !audioPlayer.isPlaying()) {
                    emit(VoicePlaybackState.Playing(voiceMessage.duration))
                    voiceRepository.markVoiceMessageAsRead(voiceMessage.id)
                    delay(1000) // 비즈니스 요구사항: 1초 대기 후 닫기
                    emit(VoicePlaybackState.Completed)
                }

            } catch (e: Exception) {
                emit(VoicePlaybackState.Error)
            }
        }
    }

    /**
     * 플레이어 준비 대기 가드레일 로직
     */
    private suspend fun waitForPlayerReady(): Boolean {
        var waitCount = 0
        val maxWaitAttempts = 15
        while (!audioPlayer.isPlaying() && waitCount < maxWaitAttempts) {
            delay(200)
            waitCount++
        }
        return audioPlayer.isPlaying()
    }

    /**
     * 자원 해제용 함수
     */
    fun release() {
        audioPlayer.stopAudio()
    }
}