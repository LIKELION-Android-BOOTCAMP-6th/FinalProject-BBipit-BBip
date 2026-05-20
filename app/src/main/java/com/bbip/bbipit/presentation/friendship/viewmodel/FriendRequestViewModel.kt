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


@HiltViewModel
class FriendRequestViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _requestList = MutableStateFlow<List<User>>(emptyList())
    val requestList = _requestList.asStateFlow()

    init {
        loadPendingRequests()
    }

    // 요청 목록 불러오기
    fun loadPendingRequests() {
        viewModelScope.launch {
            // ※ 참고: UserRepository에 getPendingFriendRequests가 없다면,
            // 위에서 정의한 쿼리 로직을 UserRemoteDataSource에 먼저 구현해야 합니다.
            val result = userRepository.getPendingFriendRequests()
            if (result is Result.Success) {
                _requestList.value = result.data
            }
        }
    }

    // 친구 요청 수락
    fun acceptFriendRequest(targetUid: String) {
        viewModelScope.launch {
            val result = userRepository.acceptFriendRequest(targetUid)
            if (result is Result.Success) {
                loadPendingRequests() // 성공 후 리스트 갱신
            }
        }
    }

    // 친구 요청 거절
    fun rejectFriendRequest(targetUid: String) {
        viewModelScope.launch {
            val result = userRepository.declineFriendRequest(targetUid)
            if (result is Result.Success) {
                loadPendingRequests() // 성공 후 리스트 갱신
            }
        }
    }
}