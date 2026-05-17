package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.source.remote.voice.VoiceRemoteDataSource
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.VoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 음성 메시지 관련 데이터 처리를 담당하는 구현체입니다.
 * 음성 파일 업로드 및 메시지 전송, 수신 관찰 기능을 제공합니다.
 */
@Singleton
class VoiceRepositoryImpl @Inject constructor(
    private val voiceRemoteDataSource: VoiceRemoteDataSource
) : VoiceRepository {

    // 음성 메시지 전송
    override suspend fun sendVoiceMessage(
        receiverId: String,
        voiceUrl: String,
        duration: Int
    ): Result<Boolean> {
        return try {
            val isOnline = voiceRemoteDataSource.sendVoiceMessage(receiverId, voiceUrl, duration)
            Result.Success(isOnline)
        } catch (e: Exception) {
            Log.e("VoiceRepository", "음성 메시지 전송 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "음성 메시지 전송 중 오류 발생"))
        }
    }

    // 수신 음성 메시지 관찰
    override fun observeIncomingVoice(myUid: String): Flow<VoiceMessage> {
        return voiceRemoteDataSource.observeIncomingVoice(myUid).map { (id, dto) ->
            VoiceMessage(
                id = id,
                senderId = dto.senderId,
                receiverId = dto.receiverId,
                voiceUrl = dto.voiceUrl,
                duration = dto.duration,
                isRead = dto.isRead,
                createdAt = dto.createdAt?.toDate()?.time ?: 0L
            )
        }
    }

    // 음성 파일 업로드
    override suspend fun uploadVoiceFile(localFileUri: android.net.Uri): Result<String> {
        return try {
            val downloadUrl = voiceRemoteDataSource.uploadVoiceFile(localFileUri)
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e("VoiceRepository", "음성 파일 업로드 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "파일 업로드 중 오류 발생"))
        }
    }

    // 음성 메시지 직접 전송
    override suspend fun sendVoiceMessageDirect(
        senderId: String,
        receiverId: String,
        voiceUrl: String,
        duration: Int
    ): Result<Boolean> {
        return try {
            voiceRemoteDataSource.sendVoiceMessageDirect(senderId, receiverId, voiceUrl, duration)
            Result.Success(true)
        } catch (e: Exception) {
            Log.e("VoiceRepository", "음성 메시지 직접 전송 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "Direct 메시지 전송 중 오류 발생"))
        }
    }

    // 음성 메시지 읽음 처리
    override suspend fun markVoiceMessageAsRead(messageId: String): Result<Boolean> {
        return try {
            voiceRemoteDataSource.markVoiceMessageAsRead(messageId)
            Result.Success(true)
        } catch (e: Exception) {
            Log.e("VoiceRepository", "음성 메시지 읽음 처리 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "읽음 처리 중 오류 발생"))
        }
    }
}