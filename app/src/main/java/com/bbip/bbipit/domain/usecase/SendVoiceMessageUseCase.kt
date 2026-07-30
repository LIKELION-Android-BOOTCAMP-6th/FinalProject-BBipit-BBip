package com.bbip.bbipit.domain.usecase

import android.net.Uri
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

sealed interface VoiceUploadResult {
    data class FileReady(val fileUri: String) : VoiceUploadResult
    object Success : VoiceUploadResult
    data class Failure(val message: String) : VoiceUploadResult
}

/**
 * 녹음된 음성 파일을 스토리지에 업로드하고 상대방에게 무전 메시지를 전송하는 핵심 유즈케이스
 */
class SendVoiceMessageUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val voiceRepository: VoiceRepository
) {
    suspend operator fun invoke(
        audioUri: Uri?,
        duration: Int,
        targetUid: String?
    ): VoiceUploadResult {
        // 파일 기록 안정화를 위한 대기 가드레일
        delay(500)

        // 파일 유효성 검증
        if (audioUri == null) {
            return VoiceUploadResult.Failure("녹음된 파일이 없거나 유효하지 않습니다.")
        }

        val myUid = authRepository.getCurrentUserUid()

        // 인증 및 수신자 정보 유효성 확인
        if (myUid == null || targetUid == null) {
            return VoiceUploadResult.Failure("사용자 인증 정보 또는 수신자 정보가 올바르지 않습니다.")
        }

        // 비즈니스 정책: 전송 딜레이 보정을 위해 오디오 길이에 1초 추가
        val correctedDuration = duration + 1

        // 스토리지 파일 업로드 실행
        val uploadResult = voiceRepository.uploadVoiceFile(myUid, audioUri)
        var finalResult: VoiceUploadResult = VoiceUploadResult.Failure("알 수 없는 오류 발생")

        uploadResult.onSuccess { url ->
            // 상대방에게 무전 메시지 최종 전송
            val sendResult = voiceRepository.sendVoiceMessage(targetUid, url, correctedDuration)

            sendResult.onSuccess {
                finalResult = VoiceUploadResult.Success
            }.onFailure { e ->
                finalResult = VoiceUploadResult.Failure(e.message ?: "메시지 전송에 실패했습니다.")
            }
        }.onFailure { e ->
            finalResult = VoiceUploadResult.Failure("스토리지 업로드 실패: ${e.message}")
        }

        return finalResult
    }
}