package com.bbip.bbipit.data.source.remote.voice

import com.bbip.bbipit.data.source.model.VoiceMessageDto
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 음성 메시지 관련 원격 데이터 소스 구현체입니다.
 * Firebase Storage 및 Firestore를 통해 음성 파일 업로드와 메시지 전송 기능을 처리합니다.
 */
@Singleton
class VoiceRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage
) : VoiceRemoteDataSource {

    // 음성 메시지 전송
    override suspend fun sendVoiceMessage(receiverId: String, voiceUrl: String, duration: Int): Boolean {
        val data = hashMapOf(
            "receiverId" to receiverId,
            "voiceUrl" to voiceUrl,
            "duration" to duration
        )
        val result = functions.getHttpsCallable("sendVoiceMessage").call(data).await()
        return (result.data as? Map<*, *>)?.get("isOnline") as? Boolean ?: false
    }

    // 수신 음성 메시지 실시간 관찰
    override fun observeIncomingVoice(myUid: String): Flow<Pair<String, VoiceMessageDto>> = callbackFlow {
        val query = firestore.collection("VoiceMessages")
            .document(myUid)
            .collection("Messages")
            .orderBy("sent_at", Query.Direction.ASCENDING)

        val subscription = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            snapshot?.documentChanges?.forEach { dc ->
                if (dc.type == DocumentChange.Type.ADDED) {
                    val dto = dc.document.toObject(VoiceMessageDto::class.java)
                    if (dto != null && dto.voiceUrl.isNotEmpty()) {
                        trySend(Pair(dc.document.id, dto))
                    }
                }
            }
        }
        awaitClose { subscription.remove() }
    }

    // 음성 파일 업로드
    override suspend fun uploadVoiceFile(localFileUri: android.net.Uri): String {
        val fileName = "voices/${UUID.randomUUID()}.m4a"
        val voiceRef = storage.reference.child(fileName)
        return voiceRef.putFile(localFileUri).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            voiceRef.downloadUrl
        }.await().toString()
    }

    // 음성 메시지 직접 전송
    override suspend fun sendVoiceMessageDirect(senderId: String, receiverId: String, voiceUrl: String, duration: Int) {
        val messageData = hashMapOf(
            "sender_id" to senderId,
            "receiver_id" to receiverId,
            "voice_url" to voiceUrl,
            "duration" to duration,
            "sent_at" to com.google.firebase.Timestamp.now(),
            "is_read" to false
        )
        firestore.collection("VoiceMessages")
            .document(receiverId)
            .collection("Messages")
            .add(messageData)
            .await()
    }

    // 음성 메시지 읽음 처리
    override suspend fun markVoiceMessageAsRead(messageId: String) {
        val data = hashMapOf("messageId" to messageId)
        functions.getHttpsCallable("markVoiceMessageAsRead").call(data).await()
    }
}