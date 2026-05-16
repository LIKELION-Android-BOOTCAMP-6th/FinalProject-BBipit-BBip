package com.bbip.bbipit.presentation.test

import android.util.Log
import androidx.lifecycle.ViewModel
import com.bbip.bbipit.domain.entity.ChatMessage
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class FeatureTestViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val voiceRepository: VoiceRepository,
    private val notiRepository: NotificationRepository
) : ViewModel() {

    private val TAG = "FeatureTest"

    /**
     * 로그인 테스트
     */
    suspend fun testLogin(email: String, pass: String): Boolean {
        return try {
            Log.d(TAG, "🔍 로그인 테스트 시작: $email")
            authRepository.signInWithEmail(email, pass)
            Log.d(TAG, "✅ 로그인 성공")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 로그인 실패: ${e.message}")
            false
        }
    }

    /**
     * 회원가입 테스트
     */
    suspend fun testSignUp(email: String, pass: String): Boolean {
        return try {
            Log.d(TAG, "🔍 회원가입 테스트 시작: $email")
            authRepository.signUpWithEmail(email, pass)
            Log.d(TAG, "✅ 회원가입 성공")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 회원가입 실패: ${e.message}")
            false
        }
    }

    /**
     * 인증 확인 테스트 (UID 조회)
     */
    fun testAuth(): String? {
        val uid = authRepository.getCurrentUserUid()
        Log.d(TAG, "🔍 인증 확인 요청: UID = ${uid ?: "없음"}")
        return uid
    }

    /**
     * Heartbeat 테스트
     */
    suspend fun testHeartbeat(roomId: String? = null): Boolean {
        return try {
            userRepository.updateHeartbeat(roomId)
            Log.d(TAG, "✅ Heartbeat 업데이트 완료 (Room: ${roomId ?: "전체"})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Heartbeat 업데이트 실패: ${e.message}")
            false
        }
    }

    /**
     * 친구 요청 테스트
     */
    suspend fun testFriendRequest(targetUid: String): Boolean {
        return try {
            val msg = userRepository.sendFriendRequest(targetUid)
            Log.d(TAG, "✅ 친구 요청 성공: $msg")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 친구 요청 실패: ${e.message}")
            false
        }
    }

    /**
     * 친구 삭제 테스트
     */
    suspend fun testDeleteFriend(targetUid: String): Boolean {
        return try {
            val msg = userRepository.deleteFriend(targetUid)
            Log.d(TAG, "✅ 친구 삭제 성공: $msg")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 친구 삭제 실패: ${e.message}")
            false
        }
    }

    /**
     * 수락된 친구 목록 조회 테스트
     */
    suspend fun testGetFriends(): List<User> {
        return try {
            val friends = userRepository.getMyAcceptedFriends()
            Log.d(TAG, "✅ 친구 목록 조회 성공: ${friends.size}명")
            friends.forEach { f ->
                Log.d(TAG, " - 👤 ${f.nickname} (ID: ${f.id}) | 상태: ${if (f.isOnline) "온라인" else "오프라인"}")
            }
            friends
        } catch (e: Exception) {
            Log.e(TAG, "❌ 친구 목록 조회 실패: ${e.message}")
            emptyList()
        }
    }

    /**
     * 채팅방 생성 테스트
     */
    suspend fun testCreateRoom(targetUid: String): Boolean {
        return try {
            val roomRes = chatRepository.createChatRoom(targetUid)
            Log.d(TAG, "✅ 채팅방 생성 성공: RoomID=${roomRes.roomId}")
            roomRes.success
        } catch (e: Exception) {
            Log.e(TAG, "❌ 채팅방 생성 실패: ${e.message}")
            false
        }
    }

    /**
     * 메시지 전송 테스트
     */
    suspend fun testSendMessage(roomId: String, targetUid: String, content: String): Boolean {
        return try {
            chatRepository.sendMessage(roomId, targetUid, content)
            Log.d(TAG, "✅ 메시지 전송 성공 (Room: $roomId, Target: $targetUid)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 전송 실패: ${e.message}")
            false
        }
    }

    /**
     * 메시지 수신 테스트 (실시간 구독)
     */
    suspend fun testObserveMessages(roomId: String): Boolean {
        return try {
            Log.d(TAG, "🔍 메시지 수신 구독 시작 (Room: $roomId)")
            chatRepository.observeMessages(roomId).collectLatest { messages ->
                Log.d(TAG, "✅ 실시간 메시지 수신됨: ${messages.size}개")
                messages.forEach { msg ->
                    Log.d(TAG, " - 💬 [${msg.senderId}]: ${msg.content}")
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 수신 테스트 중 오류 발생: ${e.message}")
            false
        }
    }

    /**
     * 채팅방 목록 조회 테스트
     */
    suspend fun testGetChatRooms(): List<ChatRoom> {
        return try {
            val rooms = chatRepository.fetchMyChatRooms()
            Log.d(TAG, "✅ 채팅방 목록 조회 성공: ${rooms.size}개")
            rooms.forEach { r ->
                Log.d(TAG, " - 🏠 RoomID: ${r.roomId}, 마지막 메시지: ${r.lastMsg}")
            }
            rooms
        } catch (e: Exception) {
            Log.e(TAG, "❌ 채팅방 목록 조회 실패: ${e.message}")
            emptyList()
        }
    }

    /**
     * 메시지 읽음 처리 테스트
     */
    suspend fun testMarkMessagesRead(roomId: String): Boolean {
        return try {
            val success = chatRepository.markMessagesAsRead(roomId)
            Log.d(TAG, "✅ 메시지 읽음 처리 완료: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 읽음 처리 실패: ${e.message}")
            false
        }
    }

    /**
     * 전체 메시지 내역 조회 테스트
     */
    suspend fun testFetchAllMessages(roomId: String): List<ChatMessage> {
        return try {
            val messages = chatRepository.fetchAllMessages(roomId)
            Log.d(TAG, "✅ 전체 메시지 내역 조회 성공: ${messages.size}개")
            messages.forEach { msg ->
                Log.d(TAG, " - 📜 [${msg.senderId}]: ${msg.content} (읽음 여부: ${msg.isRead})")
            }
            messages
        } catch (e: Exception) {
            Log.e(TAG, "❌ 전체 메시지 내역 조회 실패: ${e.message}")
            emptyList()
        }
    }

    /**
     * 이미 업로드된 음성 URL 전송 테스트
     */
    suspend fun testSendVoiceUrl(targetUid: String, voiceUrl: String): Boolean {
        val result = voiceRepository.sendVoiceMessage(targetUid, voiceUrl, 5)
        if (result.isSuccess) {
            Log.d(TAG, "✅ 음성 URL 전송 성공: $voiceUrl")
        } else {
            Log.e(TAG, "❌ 음성 URL 전송 실패")
        }
        return result.isSuccess
    }

    /**
     * 음성 수신 테스트 (실시간 구독)
     */
    suspend fun testObserveVoice(): Boolean {
        return try {
            val myUid = authRepository.getCurrentUserUid() ?: return false
            Log.d(TAG, "🔍 음성 수신 구독 시작 (UID: $myUid)")
            voiceRepository.observeIncomingVoice(myUid).collectLatest { voiceMsg ->
                Log.d(TAG, "✅ 실시간 음성 수신됨: 발신자=${voiceMsg.senderId}, 경로=${voiceMsg.voiceUrl}")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 음성 수신 테스트 중 오류 발생: ${e.message}")
            false
        }
    }

    /**
     * 알림 읽음 처리 테스트
     */
    suspend fun testMarkNotiRead(type: String, notiId: String? = null): Boolean {
        return notiRepository.markNotificationsAsRead(type, notiId)
            .onSuccess { Log.d(TAG, "✅ 알림 읽음 처리 성공") }
            .onFailure { Log.e(TAG, "❌ 알림 읽음 처리 실패: ${it.message}") }
            .isSuccess
    }

    /**
     * 알림 삭제 테스트
     */
    suspend fun testDeleteNoti(type: String, notiId: String? = null): Boolean {
        return notiRepository.deleteNotifications(type, notiId)
            .onSuccess { Log.d(TAG, "✅ 알림 삭제 성공") }
            .onFailure { Log.e(TAG, "❌ 알림 삭제 실패: ${it.message}") }
            .isSuccess
    }

    /**
     * 온라인/오프라인 상태 업데이트 테스트
     */
    suspend fun testUpdateOnlineStatus(isOnline: Boolean): Boolean {
        val success = userRepository.updateOnlineStatus(isOnline)
        Log.d(TAG, "✅ 온라인 상태 업데이트(${if (isOnline) "온라인" else "오프라인"}) 성공: $success")
        return success
    }

    /**
     * 친구 요청 수락 테스트
     */
    suspend fun testAcceptFriend(targetUid: String): Boolean {
        val success = userRepository.acceptFriendRequest(targetUid)
        Log.d(TAG, "✅ 친구 수락 성공 (UID: $targetUid)")
        return success
    }

    /**
     * 친구 요청 거절 테스트
     */
    suspend fun testDeclineFriend(targetUid: String): Boolean {
        val success = userRepository.declineFriendRequest(targetUid)
        Log.d(TAG, "✅ 친구 거절 성공 (UID: $targetUid)")
        return success
    }

    /**
     * 프로필 업데이트 테스트
     */
    suspend fun testUpdateProfile(nickname: String, status: String, profileImageUrl: String): Boolean {
        return try {
            userRepository.updateProfile(nickname, status, profileImageUrl)
            Log.d(TAG, "✅ 프로필 업데이트 성공: 닉네임=$nickname, 상태=$status")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 프로필 업데이트 실패: ${e.message}")
            false
        }
    }

    /**
     * 특정 유저 상세(User) 데이터 조회 테스트
     */
    suspend fun testGetUserProfile(targetUid: String): User? {
        Log.d(TAG, "🔍 특정 유저 프로필 조회 시작: $targetUid")
        return userRepository.getUserProfile(targetUid)
            .onSuccess { user ->
                Log.d(TAG, "✅ 유저 프로필 조회 성공: $user")
            }
            .onFailure { e ->
                Log.e(TAG, "❌ 유저 프로필 조회 실패: ${e.message}")
            }
            .getOrNull()
    }

    /**
     * 친구 유저 데이터 조회 테스트
     */
    suspend fun fetchFriendProfile(targetUid: String): User? {
        Log.d(TAG, "🔍 친구 프로필 조회 시작: $targetUid")
        return userRepository.getFriendProfileWithStatus(targetUid)
            .onSuccess { (user, status) ->
                Log.d(TAG, "✅ 친구 프로필 조회 성공: $user, 관계 상태: $status")
            }
            .onFailure { e ->
                Log.e(TAG, "❌ 친구 프로필 조회 실패: ${e.message}")
            }
            .getOrNull()?.first
    }
}
