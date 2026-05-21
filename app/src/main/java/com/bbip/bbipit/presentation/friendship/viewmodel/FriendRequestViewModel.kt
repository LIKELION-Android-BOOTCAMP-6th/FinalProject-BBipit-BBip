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


@HiltViewModel
class FriendRequestViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

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

            userRepository.myFriends.collect { friends ->
                _requestList.value = friends.filter { it.status == "requested" }
            }
        }
    }

    // 친구 요청 수락
    fun acceptFriendRequest(targetUid: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = userRepository.acceptFriendRequest(targetUid)

            result.onSuccess {
                android.util.Log.d("FriendRequestViewModel", "수락 성공: $targetUid")

            }.onFailure { appError ->
                android.util.Log.e("FriendRequestViewModel", "수락 실패: ${appError.message}")
            }

            _isLoading.value = false
        }
    }

    // 친구 요청 거절
    fun rejectFriendRequest(targetUid: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = userRepository.declineFriendRequest(targetUid)

            result.onSuccess {
                android.util.Log.d("FriendRequestViewModel", "거절 성공: $targetUid")

            }.onFailure { appError ->
                android.util.Log.e("FriendRequestViewModel", "거절 실패: ${appError.message}")
            }

            _isLoading.value = false
        }
    }
}