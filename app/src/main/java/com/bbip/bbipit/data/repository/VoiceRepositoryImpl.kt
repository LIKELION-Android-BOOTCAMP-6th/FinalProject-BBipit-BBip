package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.mapper.toDomainEntity
import com.bbip.bbipit.data.source.remote.voice.VoiceRemoteDataSource
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.VoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 음성 메시지 및 무전 데이터 처리
 */
@Singleton
class VoiceRepositoryImpl @Inject constructor(
    private val voiceRemoteDataSource: VoiceRemoteDataSource
) : VoiceRepository {

    private val _voiceMessageEvent = MutableSharedFlow<VoiceMessage>(extraBufferCapacity = 1)
    override val voiceMessageEvent = _voiceMessageEvent.asSharedFlow()

    override suspend fun getVoiceMessageById(messageId: String): Result<VoiceMessage> {
        return try {
            val dto = voiceRemoteDataSource.getVoiceMessageById(messageId)

            val voiceMessage = dto.toDomainEntity(id = messageId, isInitial = false)

            Result.Success(voiceMessage)
        } catch (e: Exception) {
            Log.e("VoiceRepository", "음성 메시지 단건 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "음성 메시지를 가져오는 중 오류 발생"))
        }
    }

    /**
     * 모바일용 무전 수신 이벤트 송출 함수
     */
    override suspend fun emitMobileVoiceEvent(message: VoiceMessage) {
        // 이벤트를 SharedFlow로 전달
        _voiceMessageEvent.emit(message)
    }

    /**
     * 음성 메시지 전송 함수
     */
    override suspend fun sendVoiceMessage(
        receiverId: String,
        voiceUrl: String,
        duration: Int
    ): Result<Boolean> {
        return try {
            // 원격 서버에 음성 메시지 전송
            val isOnline = voiceRemoteDataSource.sendVoiceMessage(receiverId, voiceUrl, duration)
            Result.Success(isOnline)
        } catch (e: Exception) {
            // 전송 실패 예외 처리
            Log.e("VoiceRepository", "음성 메시지 전송 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "음성 메시지 전송 중 오류 발생"))
        }
    }

    /**
     * 수신된 무전 메시지를 실시간으로 구독(관찰)하는 Flow 생성 함수
     */
    override fun observeIncomingVoice(myUid: String): Flow<VoiceMessage> {
        // 데이터 수신 후 도메인 엔티티로 변환
        return voiceRemoteDataSource.observeIncomingVoice(myUid).map { (id, dto, isInitial) ->
            VoiceMessage(
                id = id,
                senderId = dto.senderId,
                senderName = dto.senderName,
                senderProfileUrl = dto.senderProfileUrl,
                receiverId = dto.receiverId,
                voiceUrl = dto.voiceUrl,
                duration = dto.duration,
                createdAt = dto.createdAt?.toDate()?.time ?: 0L,
                isInitial = isInitial
            )
        }
    }

    /**
     * 음성 파일을 스토리지에 업로드하는 함수
     */
    override suspend fun uploadVoiceFile(localFileUri: android.net.Uri): Result<String> {
        return try {
            // 파일 업로드 후 다운로드 URL 획득
            val downloadUrl = voiceRemoteDataSource.uploadVoiceFile(localFileUri)
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            // 업로드 실패 예외 처리
            Log.e("VoiceRepository", "음성 파일 업로드 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "파일 업로드 중 오류 발생"))
        }
    }

    /**
     * 음성 메시지를 상대방에게 직접 전송하는 함수
     */
//    override suspend fun sendVoiceMessageDirect(
//        senderId: String,
//        receiverId: String,
//        voiceUrl: String,
//        duration: Int
//    ): Result<Boolean> {
//        return try {
//            // 서버 기능을 통해 메시지 직접 전송
//            voiceRemoteDataSource.sendVoiceMessageDirect(senderId, receiverId, voiceUrl, duration)
//            Result.Success(true)
//        } catch (e: Exception) {
//            // 전송 실패 예외 처리
//            Log.e("VoiceRepository", "음성 메시지 직접 전송 실패: ${e.message}")
//            Result.Failure(AppError.Unknown(e.message ?: "Direct 메시지 전송 중 오류 발생"))
//        }
//    }

    /**
     * 음성 메시지를 읽음 상태로 업데이트하는 함수
     */
    override suspend fun markVoiceMessageAsRead(messageId: String): Result<Boolean> {
        return try {
            // 원격 저장소의 읽음 상태 변경
            voiceRemoteDataSource.markVoiceMessageAsRead(messageId)
            Result.Success(true)
        } catch (e: Exception) {
            // 읽음 처리 실패 예외 처리
            Log.e("VoiceRepository", "음성 메시지 읽음 처리 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "읽음 처리 중 오류 발생"))
        }
    }
}