package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.model.ChatRoomDto
import com.bbip.bbipit.data.source.model.MessageDto
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.ChatRoomResult
import com.bbip.bbipit.domain.entity.ChatMessage
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

/**
 * 채팅 데이터 처리 구현체
 * Firebase Functions와 Firestore를 활용하여 채팅방 생성, 메시지 전송 및 실시간 동기화 수행
 */
class ChatRepositoryImpl @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions,
    private val db: FirebaseFirestore
) : ChatRepository {
    // 채팅방 생성 요청 처리
    override suspend fun createChatRoom(targetUid: String): ChatRoomResult {
        val data = hashMapOf("targetUid" to targetUid)
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("createChatRoom")
                .call(data)
                .await()
            val res = result.data as? Map<*, *>
            ChatRoomResult(
                success = res?.get("success") as? Boolean ?: false,
                roomId = res?.get("roomId") as? String,
                message = res?.get("message") as? String ?: ""
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "채팅방 개설 실패: ${e.message}")
            ChatRoomResult(false, null, e.message ?: "Unknown Error")
        }
    }
    // 메시지 전송 요청 처리
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
    // 채팅방 목록 실시간 관찰 스트림 제공
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
    // 특정 채팅방의 메시지 실시간 관찰 스트림 제공
    override fun observeMessages(roomId: String): Flow<List<ChatMessage>> =
        callbackFlow {
            val query = db.collection("DMs").document(roomId)
                .collection("Messages")
                .orderBy("sent_at", Query.Direction.ASCENDING)
            // 메모리 내 현재 메시지 리스트 유지
            val currentChatMessages = mutableListOf<ChatMessage>()
            val subscription = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    val messageDto = change.document.toObject(MessageDto::class.java)
                    val message = messageDto?.toEntity(change.document.id) ?: return@forEach
                    when (change.type) {
                        // 새 메시지 추가 시 리스트 삽입
                        DocumentChange.Type.ADDED -> {
                            currentChatMessages.add(change.newIndex, message)
                        }
                        // 메시지 수정 시 변경 사항 반영
                        DocumentChange.Type.MODIFIED -> {
                            if (change.oldIndex == change.newIndex) {
                                currentChatMessages[change.newIndex] = message
                            } else {
                                currentChatMessages.removeAt(change.oldIndex)
                                currentChatMessages.add(change.newIndex, message)
                            }
                        }
                        // 메시지 삭제 시 리스트 제거
                        DocumentChange.Type.REMOVED -> {
                            currentChatMessages.removeAt(change.oldIndex)
                        }
                    }
                }
                // 업데이트된 메시지 리스트 전송
                trySend(currentChatMessages.toList())
            }
            awaitClose { subscription.remove() }
        }
    // 채팅방 목록 일괄 가져오기
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
    // 메시지 읽음 처리 요청
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
    // 특정 채팅방의 모든 메시지 내역 일괄 가져오기
    override suspend fun fetchAllMessages(roomId: String): List<ChatMessage> {
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
                    ChatMessage(
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
