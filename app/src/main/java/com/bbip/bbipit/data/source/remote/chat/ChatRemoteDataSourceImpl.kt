package com.bbip.bbipit.data.source.remote.chat

import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.model.ChatRoomDto
import com.bbip.bbipit.data.source.model.MessageDto
import com.bbip.bbipit.domain.entity.ChatMessage
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.ChatRoomResult
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채팅 관련 원격 데이터 소스 구현체입니다.
 * Firestore 및 Cloud Functions를 통해 채팅 기능을 처리합니다.
 */
@Singleton
class ChatRemoteDataSourceImpl @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions,
    private val db: FirebaseFirestore
) : ChatRemoteDataSource {

    // 채팅방 생성 요청
    override suspend fun createChatRoom(targetUid: String): ChatRoomResult {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("createChatRoom").call(data).await()
        val res = result.data as? Map<*, *>
        return ChatRoomResult(
            success = res?.get("success") as? Boolean ?: false,
            roomId = res?.get("roomId") as? String,
            message = res?.get("message") as? String ?: ""
        )
    }

    // 메시지 전송
    override suspend fun sendMessage(roomId: String, receiverId: String, content: String): Map<String, Any>? {
        val data = hashMapOf("roomId" to roomId, "content" to content, "receiverId" to receiverId)
        val result = firebaseFunctions.getHttpsCallable("sendMessage").call(data).await()
        return result.data as? Map<String, Any>
    }

    // 채팅방 목록 실시간 관찰
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

    // 채팅방 메시지 실시간 관찰
    override fun observeMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val query = db.collection("DMs").document(roomId)
            .collection("Messages")
            .orderBy("sent_at", Query.Direction.ASCENDING)

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
                    DocumentChange.Type.ADDED -> {
                        currentChatMessages.add(change.newIndex, message)
                    }
                    DocumentChange.Type.MODIFIED -> {
                        if (change.oldIndex == change.newIndex) {
                            currentChatMessages[change.newIndex] = message
                        } else {
                            currentChatMessages.removeAt(change.oldIndex)
                            currentChatMessages.add(change.newIndex, message)
                        }
                    }
                    DocumentChange.Type.REMOVED -> {
                        currentChatMessages.removeAt(change.oldIndex)
                    }
                }
            }
            trySend(currentChatMessages.toList())
        }
        awaitClose { subscription.remove() }
    }

    // 채팅방 목록 조회
    override suspend fun fetchMyChatRooms(): List<ChatRoom> {
        val result = firebaseFunctions.getHttpsCallable("getMyChatRooms").call().await()
        val res = result.data as? Map<*, *>
        if (res?.get("success") == true) {
            val roomsMapList = res["rooms"] as? List<Map<String, Any>> ?: emptyList()
            return roomsMapList.map { map ->
                ChatRoom(
                    roomId = map["roomId"] as? String ?: "",
                    lastMsg = map["lastMessage"] as? String ?: "",
                    participants = map["participants"] as? List<String> ?: emptyList(),
                    updatedAt = (map["lastMessageAt"] as? Number)?.toLong() ?: 0L
                )
            }
        }
        return emptyList()
    }

    // 메시지 읽음 처리
    override suspend fun markMessagesAsRead(roomId: String): Boolean {
        val data = hashMapOf("roomId" to roomId)
        val result = firebaseFunctions.getHttpsCallable("markMessagesAsRead").call(data).await()
        val res = result.data as? Map<*, *>
        return res?.get("success") as? Boolean ?: false
    }

    // 모든 메시지 내역 조회
    override suspend fun fetchAllMessages(roomId: String): List<ChatMessage> {
        val data = hashMapOf("roomId" to roomId)
        val result = firebaseFunctions.getHttpsCallable("getAllMessages").call(data).await()
        val res = result.data as? Map<*, *>
        if (res?.get("success") == true) {
            val messagesList = res["messages"] as? List<Map<String, Any>> ?: emptyList()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return messagesList.map { map ->
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
        }
        return emptyList()
    }
}