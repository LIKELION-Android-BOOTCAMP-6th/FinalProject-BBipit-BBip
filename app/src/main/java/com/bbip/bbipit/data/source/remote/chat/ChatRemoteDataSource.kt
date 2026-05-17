package com.bbip.bbipit.data.source.remote.chat

import com.bbip.bbipit.domain.entity.ChatMessage
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.ChatRoomResult
import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {
    suspend fun createChatRoom(targetUid: String): ChatRoomResult
    suspend fun sendMessage(roomId: String, receiverId: String, content: String): Map<String, Any>?
    fun observeChatRooms(myUid: String): Flow<List<ChatRoom>>
    fun observeMessages(roomId: String): Flow<List<ChatMessage>>
    suspend fun fetchMyChatRooms(): List<ChatRoom>
    suspend fun markMessagesAsRead(roomId: String): Boolean
    suspend fun fetchAllMessages(roomId: String): List<ChatMessage>
}