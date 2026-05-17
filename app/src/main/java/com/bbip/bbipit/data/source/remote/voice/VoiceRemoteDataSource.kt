package com.bbip.bbipit.data.source.remote.voice

import com.bbip.bbipit.data.source.model.VoiceMessageDto
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow

interface VoiceRemoteDataSource {
    suspend fun sendVoiceMessage(receiverId: String, voiceUrl: String, duration: Int): Boolean
    fun observeIncomingVoice(myUid: String): Flow<Pair<String, VoiceMessageDto>>
    suspend fun uploadVoiceFile(localFileUri: android.net.Uri): String
    suspend fun sendVoiceMessageDirect(senderId: String, receiverId: String, voiceUrl: String, duration: Int)
    suspend fun markVoiceMessageAsRead(messageId: String)
}