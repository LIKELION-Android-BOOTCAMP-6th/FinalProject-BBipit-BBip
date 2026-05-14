package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.ChatRoomResponse
import com.bbip.bbipit.domain.entity.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /**
     * 상대방과의 1:1 채팅방 개설을 요청합니다.
     */
    suspend fun createChatRoom(targetUid: String): ChatRoomResponse

    /**
     * 서버의 sendMessage Callable 함수를 호출합니다.
     * @return 성공 여부 및 상대방 상태가 포함된 Map
     */
    suspend fun sendMessage(roomId: String, receiverId: String, content: String): Map<String, Any>?

    // 사용자가 참여 중인 채팅방 목록 실시간 관찰
    fun observeChatRooms(myUid: String): Flow<List<ChatRoom>>

    // 특정 채팅방의 메시지 내역 실시간 관찰
    fun observeMessages(roomId: String): Flow<List<Message>>

    /**
     * 내 채팅방 목록을 서버에서 일회성으로 가져옵니다.
     */
    suspend fun fetchMyChatRooms(): List<ChatRoom>

    /**
     * 특정 채팅방의 모든 메시지를 읽음 처리합니다.
     */
    suspend fun markMessagesAsRead(roomId: String): Boolean

    /**
     * 특정 채팅방의 모든 메시지 내역을 서버에서 일회성으로 가져옵니다.
     */
    suspend fun fetchAllMessages(roomId: String): List<Message>
}