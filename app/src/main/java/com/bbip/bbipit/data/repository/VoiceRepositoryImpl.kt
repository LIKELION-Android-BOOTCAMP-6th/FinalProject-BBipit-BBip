package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.source.remote.voice.VoiceRemoteDataSource
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.VoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 무전 음성 물리 파일 업로드, 실시간 메시지 트래픽 전송, 스트림 모니터링 연동 총괄 관리 도메인 계층 저장소 구현체 클래스
 */
@Singleton
class VoiceRepositoryImpl @Inject constructor(
    private val voiceRemoteDataSource: VoiceRemoteDataSource
) : VoiceRepository {

    // 스마트폰 런타임 환경 내부 표출용 인입 무전 이벤트 데이터 일시 중계 공유 목적의 공유 흐름 파이프
    private val _voiceMessageEvent = MutableSharedFlow<VoiceMessage>(extraBufferCapacity = 1)
    override val voiceMessageEvent = _voiceMessageEvent.asSharedFlow()

    // 인입 무전 패킷 수신 기반 모바일 알림 팝업 혹은 뷰 레이어 대상 이벤트 수동 송출 중계 내부 처리 함수
    override suspend fun emitMobileVoiceEvent(message: VoiceMessage) {
        _voiceMessageEvent.emit(message)
    }

    /**
     * 지정 상대방 식별자, 인코딩 완료 클라우드 파일 경로 및 재생 시간 규격 토대 메시지 최종 전송 함수
     */
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

    /**
     * 인입 무전 메시지 데이터베이스 트래픽 필터링 및 도메인 엔티티 변환 구조 래핑 기반 지속 관찰 스트림 변환 함수
     */
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

    /**
     * 스마트폰 또는 스마트워치 캐시 폴더 내 녹음 완료 로컬 미디어 URI 대상 원격 스토리지 클라우드 공간 물리 업로드 가속 함수
     */
    override suspend fun uploadVoiceFile(localFileUri: android.net.Uri): Result<String> {
        return try {
            val downloadUrl = voiceRemoteDataSource.uploadVoiceFile(localFileUri)
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e("VoiceRepository", "음성 파일 업로드 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "파일 업로드 중 오류 발생"))
        }
    }

    /**
     * 발신 및 수신 식별자 수동 강제 매핑 기반 원격 게이트웨이 영역 대상 클라우드 미디어 주소 다이렉트 푸시 전송 함수
     */
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

    /**
     * 특정 음성 무전 메시지 청취 완료 플래그 상태 확인 및 서버 측 읽음 인덱스 필드 참(true) 갱신 수립 확인 처리 함수
     */
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