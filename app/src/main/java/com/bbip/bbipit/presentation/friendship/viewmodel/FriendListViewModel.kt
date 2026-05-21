package com.bbip.bbipit.presentation.friendship.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.Friend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.emptyList
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

@HiltViewModel
class FriendListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val functions: FirebaseFunctions,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    // 화면에 보여줄 친구 목록 리스트 (User 모델 사용)
    private val _friendList = MutableStateFlow<List<Friend>>(emptyList())
    val friendList = _friendList.asStateFlow()

    // 요청 개수를 담을 StateFlow 추가
    private val _requestCount = MutableStateFlow(0)
    val requestCount = _requestCount.asStateFlow()


    init {
        val myUid = auth.currentUser?.uid
        android.util.Log.d("FriendListViewModel", "init: myUid = $myUid") // 추가
        if (myUid != null) {
            observeFriends(myUid)
        } else {
            android.util.Log.e("FriendListViewModel", "로그인된 사용자가 없음!") // 추가
        }

    }

    // 로딩 상태나 에러 처리를 위한 변수 (필요 시 사용)
    private fun observeFriends(myUid: String) {
        userRepository.startObservingFriends(myUid)

        viewModelScope.launch {
            // userRepository.myFriends는 이제 'accepted'된 친구들만 들어있다고 가정합니다.
            userRepository.myFriends.collect { friends ->
                // 1. accepted 상태인 친구들만 필터링하여 리스트에 할당
                val acceptedFriends = friends.filter { it.friendshipStatus == "accepted" }
                android.util.Log.d("FriendListDebug", "데이터 업데이트! 전체 수신: ${friends.size}명, 수락된 친구: ${acceptedFriends.size}명")
                _friendList.value = acceptedFriends
            }
        }
        // 2. 요청 개수 가져오기 (요청 목록을 별도로 가져와서 개수만 세기)
        fetchRequestCount()
    }

    private fun fetchRequestCount() {
        viewModelScope.launch {
            val result = userRepository.getPendingFriendRequests()
            result.onSuccess { users ->
                _requestCount.value = users.size
                android.util.Log.d("FriendListViewModel", "요청 개수 업데이트: ${users.size}개")
            }.onFailure {
                android.util.Log.e("FriendListViewModel", "요청 개수 로드 실패")
            }
        }
    }

    fun refreshAll() {
        android.util.Log.d("FriendListViewModel", "전체 데이터 새로고침 시작")

        // 1. 요청 개수 갱신
        fetchRequestCount()

        // 2. 친구 목록 옵저빙 재시작 (필요한 경우)
        val myUid = auth.currentUser?.uid
        if (myUid != null) {
            userRepository.startObservingFriends(myUid)
        }
    }


    // 친구 요청 발송 함수
    fun sendFriendRequest(targetUid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // 파이어베이스 Callable 함수 호출
                val data = hashMapOf("targetUid" to targetUid)

                val result = functions
                    .getHttpsCallable("requestFriend")
                    .call(data)
                    .await()

                // 성공 시 UI에 알림 및 리스트 새로고침
                onSuccess()

            } catch (e: Exception) {
                // 에러 처리 (서버에서 던진 HttpsError 메시지 추출)
                val errorMessage = e.message ?: "요청 중 오류가 발생했습니다."
                onError(errorMessage)
            }
        }


    }
    fun createOrGetChatRoom(
        targetUid: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf("targetUid" to targetUid)

        functions
            .getHttpsCallable("createChatRoom")
            .call(data)
            .addOnSuccessListener { task ->
                val result = task.data as? Map<String, Any>
                val roomId = result?.get("roomId") as? String
                if (roomId != null) {
                    onSuccess(roomId)
                } else {
                    onError("채팅방 ID를 가져올 수 없습니다.")
                }
            }
            .addOnFailureListener { e ->
                if (e is FirebaseFunctionsException) {
                    // [이미 존재하는 경우 처리]
                    if (e.code == FirebaseFunctionsException.Code.ALREADY_EXISTS) {
                        val details = e.details as? Map<String, Any>
                        val existingRoomId = details?.get("roomId") as? String
                        if (existingRoomId != null) {
                            onSuccess(existingRoomId) // 기존 방으로 이동
                        } else {
                            onError("이미 방이 존재하지만 ID를 찾을 수 없습니다.")
                        }
                    } else {
                        onError(e.message ?: "알 수 없는 서버 오류가 발생했습니다.")
                    }
                } else {
                    onError("네트워크 연결을 확인해주세요.")
                }
            }

    }
}