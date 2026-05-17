package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.ChatRoomResult
import com.bbip.bbipit.domain.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * 채팅 관련 데이터 처리를 담당하는 리포지토리입니다.
 * 채팅방 관리 및 메시지 송수신 처리를 수행합니다.
 */
interface ChatRepository {
    // 채팅방 개설
    suspend fun createChatRoom(targetUid: String): Result<ChatRoomResult>

    // 메시지 전송
    suspend fun sendMessage(roomId: String, receiverId: String, content: String): Result<Map<String, Any>?>

    // 채팅방 목록 관찰
    fun observeChatRooms(myUid: String): Flow<List<ChatRoom>>

    // 메시지 내역 관찰
    fun observeMessages(roomId: String): Flow<List<ChatMessage>>

    // 채팅방 목록 조회
    suspend fun fetchMyChatRooms(): Result<List<ChatRoom>>

    // 메시지 읽음 처리
    suspend fun markMessagesAsRead(roomId: String): Result<Boolean>

    // 메시지 내역 조회
    suspend fun fetchAllMessages(roomId: String): Result<List<ChatMessage>>
}