package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.VoiceMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 무전 음성 물리 파일 원격 업로드, 메시지 전송 및 수신 패킷 관찰 처리 추상화 도메인 계층 리포지토리 인터페이스
 */
interface VoiceRepository {

    // 특정 수신자 식별 명세, 업로드 완료 클라우드 파일 주소 및 재생 길이 기반 음성 무전 최종 발신 전송 함수
    suspend fun sendVoiceMessage(receiverId: String, voiceUrl: String, duration: Int): Result<Boolean>

    // 인입 수신 상대방 음성 무전 메시지 데이터 스트림 상시 모니터링 구독 데이터 흐름 함수
    fun observeIncomingVoice(myUid: String): Flow<VoiceMessage>

    // 단말 로컬 하드웨어 영역 임시 기록 무전 녹음 파일 URI 대상 클라우드 원격 저장소 공간 물리 업로드 함수
    suspend fun uploadVoiceFile(localFileUri: android.net.Uri): Result<String>

    // 발신자 및 수신자 주소 명세 직접 수동 강제 대입 기반 중앙 데이터 저장소 채널 무전 메시지 직통 전송 함수
    suspend fun sendVoiceMessageDirect(senderId: String, receiverId: String, voiceUrl: String, duration: Int): Result<Boolean>

    // 특정 음성 메시지 패킷 청취 완료 플래그 필드 참(true) 상태 강제 갱신 수립 확인 처리 함수
    suspend fun markVoiceMessageAsRead(messageId: String): Result<Boolean>

    // 모바일 내부 런타임 환경 표출 필요 신규 음성 이벤트 중계 공유 목적 공유 흐름 파이프 변수
    val voiceMessageEvent: SharedFlow<VoiceMessage>

    // 인입 무전 패킷 감지 기반 모바일 전용 수신 팝업 혹은 알림 계층 이벤트 수동 송출 중계 처리 함수
    suspend fun emitMobileVoiceEvent(message: VoiceMessage)
}