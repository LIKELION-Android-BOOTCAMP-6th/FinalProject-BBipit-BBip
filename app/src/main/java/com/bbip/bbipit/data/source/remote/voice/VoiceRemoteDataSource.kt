package com.bbip.bbipit.data.source.remote.voice

import com.bbip.bbipit.data.source.model.VoiceMessageDto
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * 음성 메시지 관련 원격 데이터 소스 인터페이스
 */
interface VoiceRemoteDataSource {

    /**
     * 음성 메시지 전송 함수
     */
    suspend fun sendVoiceMessage(receiverId: String, voiceUrl: String, duration: Int): Boolean

    /**
     * 수신된 음성 메시지를 실시간으로 구독(관찰)하는 Flow 생성 함수
     */
    fun observeIncomingVoice(myUid: String): Flow<Triple<String, VoiceMessageDto, Boolean>>

    /**
     * 음성 파일을 스토리지에 업로드하는 함수
     */
    suspend fun uploadVoiceFile(localFileUri: android.net.Uri): String

    /**
     * 음성 메시지를 상대방에게 직접 전송하는 함수
     */
//    suspend fun sendVoiceMessageDirect(senderId: String, receiverId: String, voiceUrl: String, duration: Int)

    /**
     * 음성 메시지를 읽음 상태로 업데이트하는 함수
     */
    suspend fun markVoiceMessageAsRead(messageId: String)
}