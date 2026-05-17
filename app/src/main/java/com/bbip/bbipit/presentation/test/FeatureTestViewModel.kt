package com.bbip.bbipit.presentation.test

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.domain.entity.ChatMessage
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * 기능별 테스트를 수행하기 위한 ViewModel입니다.
 * 앱의 주요 로직에 대한 검증 기능을 제공합니다.
 */
@HiltViewModel
class FeatureTestViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val voiceRepository: VoiceRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val TAG = "FeatureTest"

    // 로그인 테스트
    suspend fun testLogin(email: String, pass: String): Boolean {
        Log.d(TAG, "🔍 로그인 테스트 시작: $email")
        var isLoginSuccess = false
        authRepository.signInWithEmail(email, pass)
            .onSuccess {
                Log.d(TAG, "✅ 로그인 성공")
                isLoginSuccess = true
            }
            .onFailure {
                Log.e(TAG, "❌ 로그인 실패: ${it.message}")
            }
        return isLoginSuccess
    }

    // 회원가입 테스트
    suspend fun testSignUp(email: String, pass: String): Boolean {
        Log.d(TAG, "🔍 회원가입 테스트 시작: $email")
        var isSignUpSuccess = false
        authRepository.signUpWithEmail(email, pass)
            .onSuccess {
                Log.d(TAG, "✅ 회원가입 성공")
                isSignUpSuccess = true
            }
            .onFailure {
                Log.e(TAG, "❌ 회원가입 실패: ${it.message}")
            }
        return isSignUpSuccess
    }

    // 인증 확인 테스트
    fun testAuth(): String? {
        val uid = authRepository.getCurrentUserUid()
        Log.d(TAG, "🔍 인증 확인 요청: UID = ${uid ?: "없음"}")
        return uid
    }

    // 생명주기 업데이트 테스트
    suspend fun testHeartbeat(roomId: String? = null): Boolean {
        var isHeartbeatSuccess = false
        userRepository.updateHeartbeat(roomId)
            .onSuccess {
                Log.d(TAG, "✅ Heartbeat 업데이트 완료 (Room: ${roomId ?: "전체"})")
                isHeartbeatSuccess = true
            }
            .onFailure {
                Log.e(TAG, "❌ Heartbeat 업데이트 실패: ${it.message}")
            }
        return isHeartbeatSuccess
    }

    // 친구 요청 테스트
    suspend fun testFriendRequest(targetUid: String): Boolean {
        var isRequestSuccess = false
        userRepository.sendFriendRequest(targetUid)
            .onSuccess { msg ->
                Log.d(TAG, "✅ 친구 요청 성공: $msg")
                isRequestSuccess = true
            }
            .onFailure {
                Log.e(TAG, "❌ 친구 요청 실패: ${it.message}")
            }
        return isRequestSuccess
    }

    // 친구 삭제 테스트
    suspend fun testDeleteFriend(targetUid: String): Boolean {
        var isDeleteSuccess = false
        userRepository.deleteFriend(targetUid)
            .onSuccess { msg ->
                Log.d(TAG, "✅ 친구 삭제 성공: $msg")
                isDeleteSuccess = true
            }
            .onFailure {
                Log.e(TAG, "❌ 친구 삭제 실패: ${it.message}")
            }
        return isDeleteSuccess
    }

    // 친구 목록 조회 테스트
    suspend fun testGetFriends(): List<User>? {
        var friendList: List<User>? = null
        userRepository.getMyAcceptedFriends()
            .onSuccess { friends ->
                Log.d(TAG, "✅ 친구 목록 조회 성공: ${friends.size}명")
                friends.forEach { f ->
                    Log.d(TAG, " - 👤 ${f.nickname} (ID: ${f.id}) | 상태: ${if (f.isOnline) "온라인" else "오프라인"}")
                }
                friendList = friends
            }
            .onFailure {
                Log.e(TAG, "❌ 친구 목록 조회 실패: ${it.message}")
            }
        return friendList
    }

    // 채팅방 생성 테스트
    suspend fun testCreateRoom(targetUid: String): Boolean {
        var isCreateSuccess = false
        chatRepository.createChatRoom(targetUid)
            .onSuccess { roomRes ->
                Log.d(TAG, "✅ 채팅방 생성 성공: RoomID=${roomRes.roomId}")
                isCreateSuccess = roomRes.success
            }
            .onFailure {
                Log.e(TAG, "❌ 채팅방 생성 실패: ${it.message}")
            }
        return isCreateSuccess
    }

    // 메시지 전송 테스트
    suspend fun testSendMessage(roomId: String, targetUid: String, content: String): Boolean {
        var isSendSuccess = false
        chatRepository.sendMessage(roomId, targetUid, content)
            .onSuccess {
                Log.d(TAG, "✅ 메시지 전송 성공 (Room: $roomId, Target: $targetUid)")
                isSendSuccess = true
            }
            .onFailure {
                Log.e(TAG, "❌ 메시지 전송 실패: ${it.message}")
            }
        return isSendSuccess
    }

    // 메시지 수신 테스트
    suspend fun testObserveMessages(roomId: String, onMessageReceived: (List<ChatMessage>) -> Unit): Boolean {
        return try {
            Log.d(TAG, "🔍 메시지 수신 구독 시작 (Room: $roomId)")
            chatRepository.observeMessages(roomId).collectLatest { messages ->
                Log.d(TAG, "✅ 실시간 메시지 수신됨: ${messages.size}개")
                messages.forEach { msg ->
                    Log.d(TAG, " - 💬 [${msg.senderId}]: ${msg.content}")
                }
                onMessageReceived(messages)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 수신 테스트 중 오류 발생: ${e.message}")
            false
        }
    }

    // 채팅방 목록 조회 테스트
    suspend fun testGetChatRooms(): List<ChatRoom>? {
        var roomList: List<ChatRoom>? = null
        chatRepository.fetchMyChatRooms()
            .onSuccess { rooms ->
                Log.d(TAG, "✅ 채팅방 목록 조회 성공: ${rooms.size}개")
                rooms.forEach { r ->
                    Log.d(TAG, " - 🏠 RoomID: ${r.roomId}, 마지막 메시지: ${r.lastMsg}")
                }
                roomList = rooms
            }
            .onFailure {
                Log.e(TAG, "❌ 채팅방 목록 조회 실패: ${it.message}")
            }
        return roomList
    }

    // 메시지 읽음 처리 테스트
    suspend fun testMarkMessagesRead(roomId: String): Boolean {
        var isMarkSuccess = false
        chatRepository.markMessagesAsRead(roomId)
            .onSuccess { success ->
                Log.d(TAG, "✅ 메시지 읽음 처리 완료: $success")
                isMarkSuccess = success
            }
            .onFailure {
                Log.e(TAG, "❌ 메시지 읽음 처리 실패: ${it.message}")
            }
        return isMarkSuccess
    }

    // 메시지 내역 조회 테스트
    suspend fun testFetchAllMessages(roomId: String): List<ChatMessage>? {
        var messageList: List<ChatMessage>? = null
        chatRepository.fetchAllMessages(roomId)
            .onSuccess { messages ->
                Log.d(TAG, "✅ 전체 메시지 내역 조회 성공: ${messages.size}개")
                messages.forEach { msg ->
                    Log.d(TAG, " - 📜 [${msg.senderId}]: ${msg.content} (읽음 여부: ${msg.isRead})")
                }
                messageList = messages
            }
            .onFailure {
                Log.e(TAG, "❌ 전체 메시지 내역 조회 실패: ${it.message}")
            }
        return messageList
    }

    // 음성 URL 전송 테스트
    suspend fun testSendVoiceUrl(targetUid: String, voiceUrl: String): Boolean {
        var isSendSuccess = false
        voiceRepository.sendVoiceMessage(targetUid, voiceUrl, 5)
            .onSuccess {
                Log.d(TAG, "✅ 음성 URL 전송 성공: $voiceUrl")
                isSendSuccess = true
            }
            .onFailure {
                Log.e(TAG, "❌ 음성 URL 전송 실패: ${it.message}")
            }
        return isSendSuccess
    }

    // 로컬 음성 파일 업로드 및 전송 테스트
    suspend fun testUploadAndSendVoice(targetUid: String, localFileUri: Uri): String {
        var logMessage = "초기화"
        Log.d(TAG, "🔍 음성 파일 업로드 시작: $localFileUri")

        voiceRepository.uploadVoiceFile(localFileUri)
            .onSuccess { downloadUrl ->
                Log.d(TAG, "✅ 스토리지 업로드 성공 -> URL: $downloadUrl")

                voiceRepository.sendVoiceMessage(targetUid, downloadUrl, 5)
                    .onSuccess {
                        logMessage = "성공\n-> 업로드 URL: $downloadUrl"
                    }
                    .onFailure {
                        logMessage = "업로드 성공했으나 전송 실패\n-> URL: $downloadUrl"
                    }
            }
            .onFailure {
                logMessage = "❌ 업로드 실패: ${it.message}"
            }

        return logMessage
    }

    // 음성 수신 테스트
    suspend fun testObserveVoice(onVoiceReceived: (String, String) -> Unit): Boolean {
        return try {
            val myUid = authRepository.getCurrentUserUid() ?: return false
            Log.d(TAG, "🔍 음성 수신 구독 시작 (UID: $myUid)")
            voiceRepository.observeIncomingVoice(myUid).collectLatest { voiceMsg ->
                Log.d(TAG, "✅ 실시간 음성 수신됨: 발신자=${voiceMsg.senderId}, 경로=${voiceMsg.voiceUrl}")
                onVoiceReceived(voiceMsg.senderId, voiceMsg.voiceUrl)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 음성 수신 테스트 중 오류 발생: ${e.message}")
            false
        }
    }

    // 알림 수신 테스트
    suspend fun testObserveNotification(onNotificationReceived: (Notification) -> Unit): Boolean {
        return try {
            val myUid = authRepository.getCurrentUserUid() ?: return false
            Log.d(TAG, "🔍 알림 수신 구독 시작 (UID: $myUid)")

            callbackFlow {
                val registration = notificationRepository.observeNewNotification(myUid) { notification ->
                    trySend(notification)
                }
                awaitClose {
                    Log.d(TAG, "🔒 알림 구독 해제")
                    registration.remove()
                }
            }.collectLatest { notification ->
                Log.d(TAG, "✅ 실시간 새 알림 수신됨: ID=${notification.notificationId}, Type=${notification.type}")
                onNotificationReceived(notification)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 알림 수신 테스트 중 오류 발생: ${e.message}")
            false
        }
    }

    // 알림 읽음 처리 테스트
    suspend fun testMarkNotificationRead(type: String, id: String? = null): Boolean {
        var isMarkSuccess = false
        notificationRepository.markNotificationsAsRead(type, id)
            .onSuccess {
                Log.d(TAG, "✅ 알림 읽음 처리 성공")
                isMarkSuccess = true
            }
            .onFailure {
                Log.e(TAG, "❌ 알림 읽음 처리 실패: ${it.message}")
            }
        return isMarkSuccess
    }

    // 알림 삭제 테스트
    suspend fun testDeleteNotification(type: String, id: String? = null): Boolean {
        var isDeleteSuccess = false
        notificationRepository.deleteNotifications(type, id)
            .onSuccess {
                Log.d(TAG, "✅ 알림 삭제 성공")
                isDeleteSuccess = true
            }
            .onFailure {
                Log.e(TAG, "❌ 알림 삭제 실패: ${it.message}")
            }
        return isDeleteSuccess
    }

    // 온라인 상태 업데이트 테스트
    suspend fun testUpdateOnlineStatus(isOnline: Boolean): Boolean {
        var isUpdateSuccess = false
        userRepository.updateOnlineStatus(isOnline)
            .onSuccess { success ->
                Log.d(TAG, "✅ 온라인 상태 업데이트(${if (isOnline) "온라인" else "오프라인"}) 성공: $success")
                isUpdateSuccess = success
            }
            .onFailure {
                Log.e(TAG, "❌ 온라인 상태 업데이트 실패: ${it.message}")
            }
        return isUpdateSuccess
    }

    // 친구 수락 테스트
    suspend fun testAcceptFriend(targetUid: String): Boolean {
        var isAcceptSuccess = false
        userRepository.acceptFriendRequest(targetUid)
            .onSuccess { success ->
                Log.d(TAG, "✅ 친구 수락 성공 (UID: $targetUid)")
                isAcceptSuccess = success
            }
            .onFailure {
                Log.e(TAG, "❌ 친구 수락 실패: ${it.message}")
            }
        return isAcceptSuccess
    }

    // 친구 거절 테스트
    suspend fun testDeclineFriend(targetUid: String): Boolean {
        var isDeclineSuccess = false
        userRepository.declineFriendRequest(targetUid)
            .onSuccess { success ->
                Log.d(TAG, "✅ 친구 거절 성공 (UID: $targetUid)")
                isDeclineSuccess = success
            }
            .onFailure {
                Log.e(TAG, "❌ 친구 거절 실패: ${it.message}")
            }
        return isDeclineSuccess
    }

    // 프로필 업데이트 테스트
    suspend fun testUpdateProfile(nickname: String, status: String, profileImageUrl: String): Boolean {
        var isUpdateSuccess = false
        userRepository.updateProfile(nickname, status, profileImageUrl)
            .onSuccess { msg ->
                Log.d(TAG, "✅ 프로필 업데이트 성공: 닉네임=$nickname, 상태=$status, 메시지=$msg")
                isUpdateSuccess = true
            }
            .onFailure {
                Log.e(TAG, "❌ 프로필 업데이트 실패: ${it.message}")
            }
        return isUpdateSuccess
    }

    // 특정 유저 상세 정보 조회 테스트
    suspend fun testGetUserProfile(targetUid: String): User? {
        Log.d(TAG, "🔍 특정 유저 상세 데이터 조회 시작: $targetUid")
        var userData: User? = null
        userRepository.getUserProfile(targetUid)
            .onSuccess { user ->
                Log.d(TAG, "✅ 유저 상세 데이터 조회 성공: $user")
                userData = user
            }
            .onFailure { e ->
                Log.e(TAG, "❌ 유저 상세 데이터 조회 실패: ${e.message}")
            }
        return userData
    }

    // 내 정보 직접 조회 테스트
    suspend fun testGetMyProfile(): User? {
        val myUid = authRepository.getCurrentUserUid()
        if (myUid == null) {
            Log.e(TAG, "❌ 내 정보 조회 실패: 현재 로그인된 유저가 없습니다.")
            return null
        }

        Log.d(TAG, "🔍 내 Firestore 프로필 데이터 조회 시작: UID = $myUid")
        var myData: User? = null
        userRepository.getMyProfile(myUid)
            .onSuccess { user ->
                Log.d(TAG, "✅ 내 정보 조회 성공: $user")
                myData = user
            }
            .onFailure { e ->
                Log.e(TAG, "❌ 내 정보 조회 실패: ${e.message}")
            }
        return myData
    }

    // 친구 데이터 조회 테스트
    suspend fun fetchFriendProfile(targetUid: String): User? {
        Log.d(TAG, "🔍 친구 데이터 조회 시작: $targetUid")
        var friendData: User? = null
        userRepository.getFriendProfileWithStatus(targetUid)
            .onSuccess { (user, status) ->
                Log.d(TAG, "✅ 친구 데이터 조회 성공: $user, 관계 상태: $status")
                friendData = user
            }
            .onFailure { e ->
                Log.e(TAG, "❌ 친구 데이터 조회 실패: ${e.message}")
            }
        return friendData
    }
}