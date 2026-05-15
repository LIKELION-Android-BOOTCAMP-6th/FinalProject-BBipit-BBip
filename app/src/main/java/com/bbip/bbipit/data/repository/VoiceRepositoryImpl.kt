package com.bbip.bbipit.data.repository

import com.bbip.bbipit.data.source.model.VoiceMessageDto
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.repository.VoiceRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VoiceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage
) : VoiceRepository {

    override suspend fun sendVoiceMessage(receiverId: String, voiceUrl: String, duration: Int): Result<Boolean> {
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

    override fun observeIncomingVoice(myUid: String): Flow<VoiceMessage> =
        callbackFlow {
            val subscription = firestore.collection("VoiceMessages").document(myUid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        close(e)
                        return@addSnapshotListener
                    }

                    snapshot?.toObject(VoiceMessageDto::class.java)?.let { dto ->
                        if (dto.voiceUrl.isNotEmpty()) {
                            trySend(
                                VoiceMessage(
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
            awaitClose { subscription.remove() }
        }

    override suspend fun uploadAndSendVoiceMessage(
        receiverId: String,
        localFileUri: android.net.Uri,
        duration: Int
    ): Result<Boolean> {
        return try {
            val fileName = "voices/${UUID.randomUUID()}.m4a"
            val voiceRef = storage.reference.child(fileName)
            voiceRef.putFile(localFileUri).await()
            val downloadUrl = voiceRef.downloadUrl.await().toString()
            sendVoiceMessage(receiverId, downloadUrl, duration)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
