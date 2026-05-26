package com.bbip.bbipit.presentation.friendship.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.FriendRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.bbip.bbipit.core.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.emptyList
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

@HiltViewModel
class FriendListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val friendRepository: FriendRepository,
    private val functions: FirebaseFunctions
) : ViewModel() {

    // 화면에 보여줄 친구 목록 리스트 (User 모델 사용)
    private val _friendList = MutableStateFlow<List<Friend>>(emptyList())
    val friendList = _friendList.asStateFlow()

    // 요청 개수를 담을 StateFlow 추가
    private val _requestCount = MutableStateFlow(0)
    val requestCount = _requestCount.asStateFlow()


    init {
        val myUid = authRepository.getCurrentUserUid()
        android.util.Log.d("FriendListViewModel", "init: myUid = $myUid") // 추가
        if (myUid != null) {
            observeFriends(myUid)
        } else {
            android.util.Log.e("FriendListViewModel", "로그인된 사용자가 없음!") // 추가
        }

    }

    // 로딩 상태나 에러 처리를 위한 변수 (필요 시 사용)
    private fun observeFriends(myUid: String) {
        friendRepository.startObservingFriends(myUid)

        viewModelScope.launch {
            // userRepository.myFriends는 이제 'accepted'된 친구들만 들어있다고 가정합니다.
            friendRepository.myFriends.collect { friends ->
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
            val result = friendRepository.getPendingFriendRequests()
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
        val myUid = authRepository.getCurrentUserUid()
        if (myUid != null) {
            friendRepository.startObservingFriends(myUid)
        }
    }


    // 친구 요청 발송 함수
    fun sendFriendRequest(targetCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // 비즈니스 로직 실행
            val result = friendRepository.sendFriendRequest(targetCode)

            // 결과 처리
            when (result) {
                is Result.Success -> {
                    onSuccess()
                }
                is Result.Failure -> {
                    // AppError 타입에 따라 사용자에게 보여줄 메시지를 세분화할 수 있습니다.
                    onError(result.error.message ?: "알 수 없는 오류가 발생했습니다.")
                }
            }
        }
    }

    // 친구 삭제
    fun deleteFriend(targetUid: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // 서버 요청 시작
            val result = friendRepository.deleteFriend(targetUid)

            when (result) {
                is Result.Success -> {
                    refreshAll()
                    onSuccess(result.data) // Result.Success 내부의 data 필드
                }
                is Result.Failure -> {
                    // AppError 내부에 message가 있는지 확인 (보통 error.message 또는 error.toString() 사용)
                    val errorMessage = result.error.message ?: "친구 삭제 중 오류가 발생했습니다."
                    onError(errorMessage)
                }
            }
        }
    }

    // 해당 프로필 사용자와 채팅 시작
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