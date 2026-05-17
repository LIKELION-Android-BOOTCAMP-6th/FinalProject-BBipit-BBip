package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.source.remote.chat.ChatRemoteDataSource
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.ChatRoomResult
import com.bbip.bbipit.domain.entity.ChatMessage
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채팅 관련 데이터 처리를 담당하는 구현체입니다.
 * 채팅방 생성, 메시지 송수신 및 관찰 기능을 제공합니다.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatRemoteDataSource: ChatRemoteDataSource
) : ChatRepository {

    // 채팅방 생성 요청
    override suspend fun createChatRoom(targetUid: String): Result<ChatRoomResult> {
        return try {
            val result = chatRemoteDataSource.createChatRoom(targetUid)
            Result.Success(result)
        } catch (e: Exception) {
            Log.e("ChatRepository", "채팅방 개설 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "채팅방 개설 중 오류 발생"))
        }
    }

    // 메시지 전송 요청
    override suspend fun sendMessage(roomId: String, receiverId: String, content: String): Result<Map<String, Any>?> {
        return try {
            val result = chatRemoteDataSource.sendMessage(roomId, receiverId, content)
            Result.Success(result)
        } catch (e: Exception) {
            Log.e("ChatRepository", "메세지 전송 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "메세지 전송 중 오류 발생"))
        }
    }

    // 사용자 채팅방 목록 관찰
    override fun observeChatRooms(myUid: String): Flow<List<ChatRoom>> {
        return chatRemoteDataSource.observeChatRooms(myUid)
    }

    // 특정 채팅방 메시지 관찰
    override fun observeMessages(roomId: String): Flow<List<ChatMessage>> {
        return chatRemoteDataSource.observeMessages(roomId)
    }

    // 내 채팅방 목록 조회
    override suspend fun fetchMyChatRooms(): Result<List<ChatRoom>> {
        return try {
            val rooms = chatRemoteDataSource.fetchMyChatRooms()
            Result.Success(rooms)
        } catch (e: Exception) {
            Log.e("ChatRepository", "채팅방 목록 가져오기 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "채팅방 목록 조회 실패"))
        }
    }

    // 메시지 읽음 처리
    override suspend fun markMessagesAsRead(roomId: String): Result<Boolean> {
        return try {
            val isSuccess = chatRemoteDataSource.markMessagesAsRead(roomId)
            Result.Success(isSuccess)
        } catch (e: Exception) {
            Log.e("ChatRepository", "읽음 처리 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "읽음 처리 중 오류 발생"))
        }
    }

    // 특정 채팅방의 모든 메시지 조회
    override suspend fun fetchAllMessages(roomId: String): Result<List<ChatMessage>> {
        return try {
            val messages = chatRemoteDataSource.fetchAllMessages(roomId)
            Result.Success(messages)
        } catch (e: Exception) {
            Log.e("ChatRepository", "메시지 내역 가져오기 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "메시지 내역 조회 실패"))
        }
    }
}