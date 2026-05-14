package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.model.ChatRoomDto
import com.bbip.bbipit.data.source.model.MessageDto
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.ChatRoomResponse
import com.bbip.bbipit.domain.entity.Message
import com.bbip.bbipit.domain.repository.ChatRepository
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ChatRepositoryImpl @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions,
    private val db: FirebaseFirestore
) : ChatRepository {

    override suspend fun createChatRoom(targetUid: String): ChatRoomResponse {
        val data = hashMapOf("targetUid" to targetUid)
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("createChatRoom")
                .call(data)
                .await()

            val res = result.data as? Map<*, *>
            ChatRoomResponse(
                success = res?.get("success") as? Boolean ?: false,
                roomId = res?.get("roomId") as? String,
                message = res?.get("message") as? String ?: ""
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "채팅방 개설 실패: ${e.message}")
            ChatRoomResponse(false, null, e.message ?: "Unknown Error")
        }
    }

    override suspend fun sendMessage(
        roomId: String,
        receiverId: String,
        content: String
    ): Map<String, Any>? {
        val data = hashMapOf(
            "roomId" to roomId,
            "content" to content,
            "receiverId" to receiverId
        )

        return try {
            val result = firebaseFunctions
                .getHttpsCallable("sendMessage")
                .call(data)
                .await()

            val map = result.data as? Map<String, Any>
            Log.d("ChatRepository", "메세지 전송 결과: $map")
            map
        } catch (e: Exception) {
            Log.e("ChatRepository", "메세지 전송 실패: ${e.message}")
            throw e
        }
    }

    override fun observeChatRooms(myUid: String): Flow<List<ChatRoom>> = callbackFlow {
        val query = db.collection("DMs")
            .whereArrayContains("participants", myUid)
            .orderBy("updated_at", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }

            val rooms = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ChatRoomDto::class.java)?.toEntity(doc.id)
            } ?: emptyList()

            trySend(rooms)
        }

        awaitClose { subscription.remove() }
    }


    override fun observeMessages(roomId: String): Flow<List<Message>> =
        callbackFlow {
            val query = db.collection("DMs").document(roomId)
                .collection("Messages")
                .orderBy("sent_at", Query.Direction.ASCENDING)

            // 메모리에 현재 채팅 메시지 리스트를 유지합니다.
            val currentMessages = mutableListOf<Message>()

            val subscription = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    val messageDto = change.document.toObject(MessageDto::class.java)
                    val message = messageDto?.toEntity(change.document.id) ?: return@forEach

                    when (change.type) {
                        // 1. 새 메시지 추가
                        DocumentChange.Type.ADDED -> {
                            // 신규 메시지는 리스트의 적절한 위치(newIndex)에 삽입
                            currentMessages.add(change.newIndex, message)
                        }
                        // 2. 메시지 내용 수정 (예: 읽음 상태 변경 등)
                        DocumentChange.Type.MODIFIED -> {
                            if (change.oldIndex == change.newIndex) {
                                currentMessages[change.newIndex] = message
                            } else {
                                // 인덱스가 바뀌었다면 기존 위치 삭제 후 새 위치 삽입
                                currentMessages.removeAt(change.oldIndex)
                                currentMessages.add(change.newIndex, message)
                            }
                        }
                        // 3. 메시지 삭제
                        DocumentChange.Type.REMOVED -> {
                            currentMessages.removeAt(change.oldIndex)
                        }
                    }
                }

                // 변경된 부분만 반영된 '전체 리스트'의 복사본을 UI로 보냅니다.
                // (Immutable 리스트로 보내야 UI 레이어에서 안전하게 처리 가능합니다)
                trySend(currentMessages.toList())
            }

            awaitClose { subscription.remove() }
        }

    override suspend fun fetchMyChatRooms(): List<ChatRoom> {
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("getMyChatRooms")
                .call()
                .await()

            val res = result.data as? Map<*, *>
            if (res?.get("success") == true) {
                val roomsMapList = res["rooms"] as? List<Map<String, Any>> ?: emptyList()

                roomsMapList.map { map ->
                    ChatRoom(
                        roomId = map["roomId"] as? String ?: "",
                        lastMsg = map["lastMessage"] as? String ?: "",
                        participants = map["participants"] as? List<String> ?: emptyList(),
                        updatedAt = (map["lastMessageAt"] as? Number)?.toLong() ?: 0L
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "채팅방 목록 가져오기 실패: ${e.message}")
            throw e
        }
    }

    override suspend fun markMessagesAsRead(roomId: String): Boolean {
        val data = hashMapOf("roomId" to roomId)
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("markMessagesAsRead")
                .call(data)
                .await()

            val res = result.data as? Map<*, *>
            res?.get("success") as? Boolean ?: false
        } catch (e: Exception) {
            Log.e("ChatRepository", "읽음 처리 실패: ${e.message}")
            false
        }
    }

    override suspend fun fetchAllMessages(roomId: String): List<Message> {
        val data = hashMapOf("roomId" to roomId)
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("getAllMessages")
                .call(data)
                .await()

            val res = result.data as? Map<*, *>
            if (res?.get("success") == true) {
                val messagesList = res["messages"] as? List<Map<String, Any>> ?: emptyList()
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }

                messagesList.map { map ->
                    val isoDate = map["sent_at"] as? String
                    val sentAtMillis = try {
                        if (isoDate != null) sdf.parse(isoDate)?.time ?: 0L else 0L
                    } catch (e: Exception) {
                        0L
                    }

                    Message(
                        msgId = map["id"] as? String ?: "",
                        senderId = map["sender_id"] as? String ?: "",
                        content = map["content"] as? String ?: "",
                        sentAt = sentAtMillis,
                        isRead = map["is_read"] as? Boolean ?: false
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "메시지 내역 가져오기 실패: ${e.message}")
            throw e
        }
    }
}
