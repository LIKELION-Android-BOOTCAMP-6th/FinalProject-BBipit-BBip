package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.VoiceMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 음성 메시지 및 무전 데이터 처리 Repository 인터페이스
 */
interface VoiceRepository {

    // 모바일 무전 이벤트 공유 Flow
    val voiceMessageEvent: SharedFlow<VoiceMessage>

    /**
     * 음성 메시지 전송 함수
     */
    suspend fun sendVoiceMessage(receiverId: String, voiceUrl: String, duration: Int): Result<Boolean>

    /**
     * 수신된 무전 메시지를 실시간으로 구독(관찰)하는 Flow 생성 함수
     */
    fun observeIncomingVoice(myUid: String, startTimestamp: Long): Flow<VoiceMessage>

    /**
     * 음성 파일을 스토리지에 업로드하는 함수
     */
    suspend fun uploadVoiceFile(myUid: String, localFileUri: android.net.Uri): Result<String>

    /**
     * 음성 메시지를 상대방에게 직접 전송하는 함수
     */
//    suspend fun sendVoiceMessageDirect(senderId: String, receiverId: String, voiceUrl: String, duration: Int): Result<Boolean>

    /**
     * 음성 메시지를 읽음 상태로 업데이트하는 함수
     */
    suspend fun markVoiceMessageAsRead(messageId: String): Result<Boolean>

    /**
     * 모바일용 무전 수신 이벤트 송출 함수
     */
    suspend fun emitMobileVoiceEvent(message: VoiceMessage)
    suspend fun getVoiceMessageById(messageId: String): Result<VoiceMessage>
}