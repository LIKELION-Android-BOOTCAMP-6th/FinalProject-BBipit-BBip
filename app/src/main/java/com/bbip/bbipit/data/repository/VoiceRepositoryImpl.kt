package com.bbip.bbipit.data.repository

import com.bbip.bbipit.data.source.model.VoiceMessageDto
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.repository.VoiceRepository
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 음성 메시지 데이터 처리 구현체
 * Firebase Functions, Firestore, Storage를 활용한 음성 메시지 송수신 및 관리 수행
 */
class VoiceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage
) : VoiceRepository {
    // 음성 메시지 전송 요청 처리
    override suspend fun sendVoiceMessage(
        receiverId: String,
        voiceUrl: String,
        duration: Int
    ): Result<Boolean> {
        val data = hashMapOf(
            "receiverId" to receiverId,
            "voiceUrl" to voiceUrl,
            "duration" to duration
        )
        return try {
            val result = functions.getHttpsCallable("sendVoiceMessage").call(data).await()
            val isOnline = (result.data as? Map<*, *>)?.get("isOnline") as? Boolean ?: false
            Result.success(isOnline)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // 수신 음성 메시지 실시간 관찰 스트림 제공
    override fun observeIncomingVoice(myUid: String): Flow<VoiceMessage> =
        callbackFlow {
            // 사용자별 보관함 경로의 메시지 컬렉션 감시
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
                    // 신규 도착 메시지 스트림 전달
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val dto = dc.document.toObject(VoiceMessageDto::class.java)
                        if (dto.voiceUrl.isNotEmpty()) {
                            trySend(
                                VoiceMessage(
                                    id = dc.document.id,
                                    senderId = dto.senderId,
                                    receiverId = dto.receiverId,
                                    voiceUrl = dto.voiceUrl,
                                    duration = dto.duration,
                                    isRead = dto.isRead,
                                    createdAt = dto.createdAt?.toDate()?.time ?: 0L
                                )
                            )
                        }
                    }
                }
            }
            awaitClose { subscription.remove() }
        }
    // 로컬 오디오 파일 스토리지 업로드 처리
    override suspend fun uploadVoiceFile(localFileUri: android.net.Uri): Result<String> {
        return try {
            val fileName = "voices/${UUID.randomUUID()}.m4a"
            val voiceRef = storage.reference.child(fileName)
            // 파일 업로드 및 다운로드 URL 획득 체이닝
            val downloadUrl = voiceRef.putFile(localFileUri).continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                voiceRef.downloadUrl
            }.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // 음성 메시지 Firestore 직접 전송 처리
    override suspend fun sendVoiceMessageDirect(
        senderid: String,
        receiverId: String,
        voiceUrl: String,
        duration: Int
    ): Result<Boolean> {
        return try {
            val messageData = hashMapOf(
                "sender_id" to senderid,
                "receiver_id" to receiverId,
                "voice_url" to voiceUrl,
                "duration" to duration,
                "sent_at" to com.google.firebase.Timestamp.now(),
                "is_read" to false
            )
            // 메시지 문서 추가
            firestore.collection("VoiceMessages")
                .document(receiverId)
                .collection("Messages")
                .add(messageData)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // 음성 메시지 읽음 상태 업데이트 처리
    override suspend fun markVoiceMessageAsRead(messageId: String): Result<Boolean> {
        val data = hashMapOf("messageId" to messageId)
        return try {
            functions.getHttpsCallable("markVoiceMessageAsRead").call(data).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}