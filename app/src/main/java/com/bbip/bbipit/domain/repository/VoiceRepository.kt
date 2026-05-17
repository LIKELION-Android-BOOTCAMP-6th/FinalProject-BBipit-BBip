package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.VoiceMessage
import kotlinx.coroutines.flow.Flow

/**
 * 음성 메시지 관련 데이터 처리를 담당하는 리포지토리입니다.
 * 음성 파일 업로드 및 메시지 전송, 수신 처리를 수행합니다.
 */
interface VoiceRepository {
    // 음성 메시지 전송
    suspend fun sendVoiceMessage(receiverId: String, voiceUrl: String, duration: Int): Result<Boolean>

    // 음성 메시지 관찰
    fun observeIncomingVoice(myUid: String): Flow<VoiceMessage>

    // 음성 파일 업로드
    suspend fun uploadVoiceFile(localFileUri: android.net.Uri): Result<String>

    // 음성 메시지 직접 전송
    suspend fun sendVoiceMessageDirect(senderId: String, receiverId: String, voiceUrl: String, duration: Int): Result<Boolean>

    // 음성 메시지 읽음 처리
    suspend fun markVoiceMessageAsRead(messageId: String): Result<Boolean>
}