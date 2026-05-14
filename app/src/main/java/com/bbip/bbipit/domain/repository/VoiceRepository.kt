package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.domain.entity.VoiceMessage
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    // 음성 메시지 전송 (Cloud Functions 호출)
    suspend fun sendVoiceMessage(receiverId: String, voiceUrl: String, duration: Int): Result<Boolean>

    // 나에게 온 음성 메시지 실시간 관찰 (Flow)
    fun observeIncomingVoice(myUid: String): Flow<VoiceMessage>

    // 음성 파일 업로드 및 전송 결합
    suspend fun uploadAndSendVoiceMessage(receiverId: String, localFileUri: android.net.Uri, duration: Int): Result<Boolean>

}