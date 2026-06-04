package com.bbip.bbipit.data.source.remote.voice

import android.util.Log
import com.bbip.bbipit.data.mapper.toVoiceMessageDto
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
 * 음성 메시지 관련 원격 데이터 소스 구현체 클래스
 */
@Singleton
class VoiceRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage
) : VoiceRemoteDataSource {

    /**
     * 특정 음성 메시지 ID를 통해 단건 데이터를 조회하는 함수
     */
    override suspend fun getVoiceMessageById(messageId: String): VoiceMessageDto {
        val data = hashMapOf("messageId" to messageId)
        val result = functions.getHttpsCallable("getVoiceMessageById").call(data).await()

        val resultMap = result.data as? Map<*, *>
        val voiceMessageMap = resultMap?.get("voiceMessage") as? Map<*, *>
            ?: throw Exception("음성 메시지 데이터를 찾을 수 없습니다.")

        Log.d("VoiceRemoteDataSourceImpl",voiceMessageMap.toVoiceMessageDto().toString())
        return voiceMessageMap.toVoiceMessageDto()
    }

    /**
     * 음성 메시지 전송 함수
     */
    override suspend fun sendVoiceMessage(receiverId: String, voiceUrl: String, duration: Int): Boolean {
        val data = hashMapOf(
            "receiverId" to receiverId,
            "voiceUrl" to voiceUrl,
            "duration" to duration
        )
        val result = functions.getHttpsCallable("sendVoiceMessage").call(data).await()
        return (result.data as? Map<*, *>)?.get("isOnline") as? Boolean ?: false
    }

    /**
     * 수신된 음성 메시지를 실시간으로 구독(관찰)하는 Flow 생성 함수
     */
    override fun observeIncomingVoice(myUid: String, startTimestamp: Long): Flow<Triple<String, VoiceMessageDto, Boolean>> = callbackFlow {
        val query = firestore.collection("VoiceMessages")
            .document(myUid)
            .collection("Messages")
            .orderBy("sent_at", Query.Direction.ASCENDING)

        val subscription = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }

            if (snapshot == null) return@addSnapshotListener

            // 새로 추가된 문서만 필터링하여 전달
            snapshot.documentChanges.forEach { dc ->
                if (dc.type == DocumentChange.Type.ADDED) {
                    val dto = dc.document.toObject(VoiceMessageDto::class.java)
                    if (dto != null && dto.voiceUrl.isNotEmpty()) {
                        // Firestore의 Timestamp를 밀리초(ms) 단위로 변환
                        val messageSentAt = dto.createdAt?.toDate()?.time ?: 0L

                        // 메시지 송신 시간이 서비스 시작 시간보다 이후일 때만 '신규 무전(isInitial = false)'으로 판정
                        val isInitial = messageSentAt <= startTimestamp

                        trySend(Triple(dc.document.id, dto, isInitial))
                        Log.d("VoiceRemoteDataSource", "MessageId: ${dc.document.id}, IsInitial: $isInitial")
                    }
                }
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * 음성 파일을 스토리지에 업로드하는 함수
     */
    override suspend fun uploadVoiceFile(localFileUri: android.net.Uri): String {
        val fileName = "voices/${UUID.randomUUID()}.m4a"
        val voiceRef = storage.reference.child(fileName)
        return voiceRef.putFile(localFileUri).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            voiceRef.downloadUrl
        }.await().toString()
    }

    /**
     * 음성 메시지를 상대방에게 직접 전송하는 함수
     */
//    override suspend fun sendVoiceMessageDirect(senderId: String, receiverId: String, voiceUrl: String, duration: Int) {
//        val messageData = hashMapOf(
//            "sender_id" to senderId,
//            "receiver_id" to receiverId,
//            "voice_url" to voiceUrl,
//            "duration" to duration,
//            "sent_at" to com.google.firebase.Timestamp.now(),
//            "is_read" to false
//        )
//        firestore.collection("VoiceMessages")
//            .document(receiverId)
//            .collection("Messages")
//            .add(messageData)
//            .await()
//    }

    /**
     * 음성 메시지를 읽음 상태로 업데이트하는 함수
     */
    override suspend fun markVoiceMessageAsRead(messageId: String) {
        val data = hashMapOf("messageId" to messageId)
        functions.getHttpsCallable("markVoiceMessageAsRead").call(data).await()
    }
}