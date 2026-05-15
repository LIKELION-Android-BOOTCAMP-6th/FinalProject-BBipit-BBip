package com.bbip.bbipit.presentation.test

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    fun testLogin(email: String, pass: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "로그인 테스트 시작: $email")
                authRepository.signInWithEmail(email, pass)
                Log.d(TAG, "로그인 성공")
            } catch (e: Exception) {
                Log.e(TAG, "로그인 실패: ${e.message}")
            }
        }
    }

    /**
     * 회원가입 테스트
     */
    fun testSignUp(email: String, pass: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "회원가입 테스트 시작: $email")
                authRepository.signUpWithEmail(email, pass)
                Log.d(TAG, "회원가입 성공")
            } catch (e: Exception) {
                Log.e(TAG, "회원가입 실패: ${e.message}")
            }
        }
    }

    /**
     * 인증 확인 테스트 (UID 조회)
     */
    fun testAuth() {
        val uid = authRepository.getCurrentUserUid()
        Log.d(TAG, "인증 확인: UID = $uid")
    }

    /**
     * Heartbeat 테스트
     */
    fun testHeartbeat(roomId: String? = null) {
        viewModelScope.launch {
            try {
                userRepository.updateHeartbeat(roomId)
                Log.d(TAG, "Heartbeat 업데이트 완료 (Room: $roomId)")
            } catch (e: Exception) {
                Log.e(TAG, "Heartbeat 업데이트 실패: ${e.message}")
            }
        }
    }

    /**
     * 친구 요청 테스트
     */
    fun testFriendRequest(targetUid: String) {
        viewModelScope.launch {
            try {
                val msg = userRepository.sendFriendRequest(targetUid)
                Log.d(TAG, "친구 요청 결과: $msg")
            } catch (e: Exception) {
                Log.e(TAG, "친구 요청 실패: ${e.message}")
            }
        }
    }

    /**
     * 친구 삭제 테스트
     */
    fun testDeleteFriend(targetUid: String) {
        viewModelScope.launch {
            try {
                val msg = userRepository.deleteFriend(targetUid)
                Log.d(TAG, "친구 삭제 결과: $msg")
            } catch (e: Exception) {
                Log.e(TAG, "친구 삭제 실패: ${e.message}")
            }
        }
    }

    /**
     * 수락된 친구 목록 조회 테스트
     */
    fun testGetFriends() {
        viewModelScope.launch {
            try {
                val friends = userRepository.getMyAcceptedFriends()
                Log.d(TAG, "친구 목록 조회 완료: ${friends.size}명")
                friends.forEach { f ->
                    Log.d(TAG, " - ${f.nickname} (${f.id}) online=${f.isOnline}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "친구 목록 조회 실패: ${e.message}")
            }
        }
    }

    /**
     * 채팅방 생성 테스트
     */
    fun testCreateRoom(targetUid: String) {
        viewModelScope.launch {
            try {
                val roomRes = chatRepository.createChatRoom(targetUid)
                Log.d(TAG, "채팅방 생성 완료: success=${roomRes.success}, roomId=${roomRes.roomId}")
            } catch (e: Exception) {
                Log.e(TAG, "채팅방 생성 실패: ${e.message}")
            }
        }
    }

    /**
     * 메시지 전송 테스트
     */
    fun testSendMessage(roomId: String, targetUid: String, content: String) {
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(roomId, targetUid, content)
                Log.d(TAG, "메시지 전송 완료")
            } catch (e: Exception) {
                Log.e(TAG, "메시지 전송 실패: ${e.message}")
            }
        }
    }

    /**
     * 메시지 수신 테스트 (실시간 구독)
     */
    fun testObserveMessages(roomId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "메시지 수신 구독 시작 ($roomId)")
                chatRepository.observeMessages(roomId).collectLatest { messages ->
                    Log.d(TAG, "실시간 메시지 수신: ${messages.size}개")
                    messages.forEach { msg ->
                        Log.d(TAG, " - [${msg.senderId}]: ${msg.content}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "메시지 수신 테스트 실패: ${e.message}")
            }
        }
    }

    /**
     * 채팅방 목록 조회 테스트
     */
    fun testGetChatRooms() {
        viewModelScope.launch {
            try {
                val rooms = chatRepository.fetchMyChatRooms()
                Log.d(TAG, "채팅방 목록 조회 완료: ${rooms.size}개")
                rooms.forEach { r ->
                    Log.d(TAG, " - RoomID: ${r.roomId}, LastMsg: ${r.lastMsg}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "채팅방 목록 조회 실패: ${e.message}")
            }
        }
    }

    /**
     * 메시지 읽음 처리 테스트
     */
    fun testMarkMessagesRead(roomId: String) {
        viewModelScope.launch {
            try {
                val success = chatRepository.markMessagesAsRead(roomId)
                Log.d(TAG, "메시지 읽음 처리 결과: $success")
            } catch (e: Exception) {
                Log.e(TAG, "메시지 읽음 처리 실패: ${e.message}")
            }
        }
    }

    /**
     * 전체 메시지 내역 조회 테스트
     */
    fun testFetchAllMessages(roomId: String) {
        viewModelScope.launch {
            try {
                val messages = chatRepository.fetchAllMessages(roomId)
                Log.d(TAG, "전체 메시지 조회 완료: ${messages.size}개")
                messages.forEach { msg ->
                    Log.d(TAG, " - [${msg.senderId}]: ${msg.content} (Read: ${msg.isRead})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "전체 메시지 조회 실패: ${e.message}")
            }
        }
    }

    /**
     * 음성 업로드 및 전송 테스트
     */
    fun testSendVoice(targetUid: String, fileUri: Uri) {
        viewModelScope.launch {
            try {
                val voiceRes = voiceRepository.uploadAndSendVoiceMessage(targetUid, fileUri, 5)
                Log.d(TAG, "음성 업로드 및 전송 결과: ${voiceRes.isSuccess}")
            } catch (e: Exception) {
                Log.e(TAG, "음성 전송 테스트 실패: ${e.message}")
            }
        }
    }

    /**
     * 이미 업로드된 음성 URL 전송 테스트
     */
    fun testSendVoiceUrl(targetUid: String, voiceUrl: String) {
        viewModelScope.launch {
            val result = voiceRepository.sendVoiceMessage(targetUid, voiceUrl, 5)
            Log.d(TAG, "음성 URL 전송 결과: ${result.isSuccess}")
        }
    }

    /**
     * 음성 수신 테스트 (실시간 구독)
     */
    fun testObserveVoice() {
        viewModelScope.launch {
            try {
                val myUid = authRepository.getCurrentUserUid() ?: return@launch
                Log.d(TAG, "음성 수신 구독 시작 (UID: $myUid)")
                voiceRepository.observeIncomingVoice(myUid).collectLatest { voiceMsg ->
                    Log.d(TAG, "실시간 음성 수신: sender=${voiceMsg.senderId}, url=${voiceMsg.voiceUrl}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "음성 수신 테스트 실패: ${e.message}")
            }
        }
    }

    /**
     * 알림 읽음 처리 테스트
     */
    fun testMarkNotiRead(type: String, notiId: String? = null) {
        viewModelScope.launch {
            notiRepository.markNotificationsAsRead(type, notiId)
                .onSuccess { Log.d(TAG, "알림 읽음 처리 성공") }
                .onFailure { Log.e(TAG, "알림 읽음 처리 실패: ${it.message}") }
        }
    }

    /**
     * 알림 삭제 테스트
     */
    fun testDeleteNoti(type: String, notiId: String? = null) {
        viewModelScope.launch {
            notiRepository.deleteNotifications(type, notiId)
                .onSuccess { Log.d(TAG, "알림 삭제 성공") }
                .onFailure { Log.e(TAG, "알림 삭제 실패: ${it.message}") }
        }
    }

    /**
     * 온라인/오프라인 상태 업데이트 테스트
     */
    fun testUpdateOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch {
            val success = userRepository.updateOnlineStatus(isOnline)
            Log.d(TAG, "온라인 상태 업데이트($isOnline) 결과: $success")
        }
    }

    /**
     * 친구 요청 수락 테스트
     */
    fun testAcceptFriend(targetUid: String) {
        viewModelScope.launch {
            val success = userRepository.acceptFriendRequest(targetUid)
            Log.d(TAG, "친구 수락($targetUid) 결과: $success")
        }
    }

    /**
     * 친구 요청 거절 테스트
     */
    fun testDeclineFriend(targetUid: String) {
        viewModelScope.launch {
            val success = userRepository.declineFriendRequest(targetUid)
            Log.d(TAG, "친구 거절($targetUid) 결과: $success")
        }
    }

    /**
     * 프로필 업데이트 테스트
     */
    fun testUpdateProfile(nickname: String, status: String, profileImageUrl: String) {
        viewModelScope.launch {
            try {
                userRepository.updateProfile(nickname, status, profileImageUrl)
                Log.d(TAG, "✅ 프로필 업데이트 시도 완료: $nickname, $status")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 프로필 업데이트 테스트 실패: ${e.message}")
            }
        }
    }

    /**
     * 특정 유저 상세(User) 데이터 조회 테스트
     */
    fun testGetUserProfile(targetUid: String) {
        viewModelScope.launch {
            userRepository.getUserProfile(targetUid)
                .onSuccess { user ->
                    Log.d("FeatureTest", "✅ 유저 프로필 조회 성공: $user")
                }
                .onFailure { e ->
                    Log.e("FeatureTest", "❌ 유저 프로필 조회 실패: ${e.message}")
                }
        }
    }

    /**
     * 친구 유저 데이터 조회 테스트
     */
    fun fetchFriendProfile(targetUid: String) {
        viewModelScope.launch {
            Log.d(TAG, "친구 프로필 조회 테스트 시작: $targetUid")
            userRepository.getFriendProfileWithStatus(targetUid)
                .onSuccess { (user, status) ->
                    Log.d(TAG, "✅ 친구 프로필 조회 성공: $user, 관계 상태: $status")
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ 친구 프로필 조회 실패: ${e.message}")
                }
        }
    }
}
