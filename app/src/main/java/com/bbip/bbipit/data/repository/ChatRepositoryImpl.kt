package com.bbip.bbipit.data.repository

import android.util.Log
import androidx.navigation.NavController
import com.bbip.bbipit.core.base.AppLifecycleObserver
import com.bbip.bbipit.core.base.LifeCycleManager
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.source.remote.chat.ChatRemoteDataSource
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.ChatRoomResult
import com.bbip.bbipit.domain.entity.ChatMessage
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.model.ChatRoomDto
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채팅 관련 데이터 처리를 담당하는 구현체입니다.
 * 채팅방 생성, 메시지 송수신 및 관찰 기능을 제공합니다.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatRemoteDataSource: ChatRemoteDataSource,
    private val db: FirebaseFirestore,
    private val lifeCycleManager: LifeCycleManager,
) : ChatRepository {
    // 채팅방이 없으면 생성하고 이미 있는 경우 해당 채팅방을 반환
    override suspend fun createOrGetChatRoom(targetUid: String): Result<ChatRoomResult> {
        // 일단 채팅방 개설을 시도
        return try {
            val result = chatRemoteDataSource.createChatRoom(targetUid)
            Result.Success(result)
        } catch (e: Exception) {
            Log.e("ChatRepository", "채팅방 개설 실패: ${e.message}")

            // 반환되는 예외로 분기처리
            val targetException = if (e is FirebaseFunctionsException) e else e.cause
            if (targetException is FirebaseFunctionsException) {
                if (targetException.code == FirebaseFunctionsException.Code.ALREADY_EXISTS) {
                    val details = targetException.details as? Map<*, *>
                    val existingRoomId = details?.get("roomId") as? String
                    if (existingRoomId != null) {
                        // 기존 방이 있을 경우
                        return Result.Success(
                            ChatRoomResult(
                                success = true,
                                roomId = existingRoomId,
                                message = "이미 존재하는 채팅방입니다."
                            )
                        )
                    }
                }
            }
            Result.Failure(AppError.Unknown(e.message ?: "채팅방 반환 오류 발생"))
        }
    }

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
    override fun observeChatRooms(myUid: String): Flow<List<ChatRoom>> = callbackFlow {
        val listener = db.collection("DMs")
            .whereArrayContains("participants", myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "채팅방 실시간 구독 실패", error)
                    close(error) // Flow를 에러와 함께 닫음
                    return@addSnapshotListener
                }

                val chatRooms = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        // [해결] Dto로 변환 후 toEntity 확장함수 사용
                        doc.toObject(ChatRoomDto::class.java)?.toEntity(doc.id)
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "ChatRoom 파싱 실패: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(chatRooms)
            }
        awaitClose { listener.remove() }
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

    // 채팅방 진입/퇴장 관리
    override suspend fun updateActiveRoom(uid: String, roomId: String?) {
        try {
            // 'Live' 컬렉션에서 uid를 문서 ID로 사용한다고 가정할 때
            db.collection("Live").document(uid)
                .set(
                    mapOf("current_room_id" to roomId),
                    SetOptions.merge() // 기존 데이터를 유지하면서 해당 필드만 갱신
                )
                .await()
        } catch (e: Exception) {
            // 취소 예외는 무시하고, 실제 에러만 로그 남기기
            if (e !is kotlinx.coroutines.CancellationException) {
                android.util.Log.e("ChatRepositoryImpl", "Live 상태 업데이트 실패", e)
            }
            throw e
        }
    }
}