package com.bbip.bbipit.presentation.friendship.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.bbip.bbipit.core.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.emptyList
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

@HiltViewModel
class FriendListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val functions: FirebaseFunctions
) : ViewModel() {

    // 화면에 보여줄 친구 목록 리스트 (User 모델 사용)
    private val _friendList = MutableStateFlow<List<User>>(emptyList())
    val friendList = _friendList.asStateFlow()

    // 로딩 상태나 에러 처리를 위한 변수 (필요 시 사용)
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadFriendList()
    }

    fun loadFriendList() {
        viewModelScope.launch {
            _isLoading.value = true

            // 레포지토리 호출
            val result = userRepository.getMyAcceptedFriends()

            if (result is Result.Success) {
                //_friendList.value = result.data
            } else {
                // 실패 시 로그 출력 또는 에러 상태 업데이트
                //_friendList.value = emptyList()
            }

            _isLoading.value = false
        }
    }

    // 친구 요청 발송 함수
    fun sendFriendRequest(targetUid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Callable 함수 호출
                val data = hashMapOf("targetUid" to targetUid)

                val result = functions
                    .getHttpsCallable("requestFriend")
                    .call(data)
                    .await()

                // 성공 시 UI에 알림 및 리스트 새로고침
                onSuccess()
                loadFriendList() // 리스트 갱신

            } catch (e: Exception) {
                // 에러 처리 (서버에서 던진 HttpsError 메시지 추출)
                val errorMessage = e.message ?: "요청 중 오류가 발생했습니다."
                onError(errorMessage)
            }
        }
    }
}