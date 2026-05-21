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


@HiltViewModel
class FriendRequestViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _requestList = MutableStateFlow<List<User>>(emptyList())
    val requestList = _requestList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

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
            _isLoading.value = true

            val result = userRepository.acceptFriendRequest(targetUid)

            // 성공 처리
            result.onSuccess {
                loadPendingRequests() // 성공 시 리스트 갱신
            }

                // 실패 처리
                .onFailure { appError ->
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
                // 거절 성공 시 리스트 갱신 (요청 목록에서 해당 유저가 사라짐)
                loadPendingRequests()
            }.onFailure { appError ->
                // 실패 시 처리
                android.util.Log.e("FriendRequestViewModel", "거절 실패: ${appError.message}")
            }

            _isLoading.value = false
        }
    }
}