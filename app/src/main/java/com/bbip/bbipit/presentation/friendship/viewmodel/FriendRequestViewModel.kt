package com.bbip.bbipit.presentation.friendship.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.repository.FriendRepository


@HiltViewModel
class FriendRequestViewModel @Inject constructor(
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    private val _requestList = MutableStateFlow<List<Friend>>(emptyList())
    val requestList = _requestList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        observePendingRequests()
    }

    // 요청 목록 불러오기
    private fun observePendingRequests() {
        viewModelScope.launch {
            // 1. myFriends(이미 친구)를 보는 게 아니라, 요청 목록을 직접 가져오기
            val result = friendRepository.getPendingFriendRequests()

            result.onSuccess { users ->
                // 2. 받아온 User 리스트를 Friend 리스트로 변환
                val requestedFriends = users.map { user ->
                    Friend(
                        uid = user.id,
                        nickname = user.nickname,
                        profileImageUrl = user.profileImageUrl,
                        status = user.status,
                        friendshipStatus = "requested" // 이 리스트는 무조건 요청 상태임
                    )
                }

                _requestList.value = requestedFriends
                android.util.Log.d(
                    "FriendRequestViewModel",
                    "요청 목록 로드 성공: ${requestedFriends.size}명"
                )
            }.onFailure { error ->
                android.util.Log.e("FriendRequestViewModel", "요청 목록 로드 실패: ${error.message}")

                friendRepository.myFriends.collect { friends ->
                    _requestList.value = friends.filter { it.status == "requested" }
                }
            }
        }
    }

    // 친구 요청 수락
    fun acceptFriendRequest(targetUid: String) {
        val previousList = _requestList.value // 기존 리스트 보관
        _requestList.value = _requestList.value.filter { it.uid != targetUid }

        viewModelScope.launch {
            _isLoading.value = true

            val result = friendRepository.acceptFriendRequest(targetUid)

            result.onSuccess {
                android.util.Log.d("FriendRequestViewModel", "수락 성공: $targetUid")
                _errorMessage.value = "친구 요청을 수락했습니다."

            }.onFailure { appError ->
                android.util.Log.e("FriendRequestViewModel", "수락 실패: ${appError.message}")
                // 1. 상태 복구
                _requestList.value = previousList
                _errorMessage.value = "요청 처리에 실패했습니다."
            }

            _isLoading.value = false
        }
    }

    // 친구 요청 거절
    fun rejectFriendRequest(targetUid: String) {
        val previousList = _requestList.value // 기존 리스트 보관
        _requestList.value = _requestList.value.filter { it.uid != targetUid }

        viewModelScope.launch {
            _isLoading.value = true

            val result = friendRepository.declineFriendRequest(targetUid)

            result.onSuccess {
                android.util.Log.d("FriendRequestViewModel", "거절 성공: $targetUid")
                _errorMessage.value = "친구 요청을 거절했습니다."

            }.onFailure { appError ->
                android.util.Log.e("FriendRequestViewModel", "거절 실패: ${appError.message}")
                _requestList.value = previousList
                _errorMessage.value = "요청 처리에 실패했습니다."

            }

            _isLoading.value = false
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}