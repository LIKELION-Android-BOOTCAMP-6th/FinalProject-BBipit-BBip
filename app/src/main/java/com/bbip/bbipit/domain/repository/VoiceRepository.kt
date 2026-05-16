package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.domain.entity.VoiceMessage
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    // 음성 메시지 전송 (Cloud Functions 호출)
    suspend fun sendVoiceMessage(receiverId: String, voiceUrl: String, duration: Int): Result<Boolean>

    // 나에게 온 음성 메시지 실시간 관찰 (Flow)
    fun observeIncomingVoice(myUid: String): Flow<VoiceMessage>
    // 파일만 미리 업로드하고 URL을 반환
    suspend fun uploadVoiceFile(localFileUri: android.net.Uri): Result<String>

    // 이미 업로드된 URL을 사용하여 메시지만 전송
    suspend fun sendVoiceMessageDirect(senderId: String, receiverId: String, voiceUrl: String, duration: Int): Result<Boolean>

    // 특정 음성 메시지 읽음 처리
    suspend fun markVoiceMessageAsRead(messageId: String): Result<Boolean>
}