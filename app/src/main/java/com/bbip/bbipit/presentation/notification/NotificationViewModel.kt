package com.bbip.bbipit.presentation.notification

import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.bbip.bbipit.domain.usecase.GetNotificationListUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val getNotificationListUseCase: GetNotificationListUseCase,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _notification = MutableStateFlow<List<Notification>>(emptyList())
    val notification: StateFlow<List<Notification>> = _notification.asStateFlow()

    // 전체 확인 버튼 클릭 여부 (보라색 점 제거용, isRead와 무관)
    private val _readAllClicked = MutableStateFlow(false)
    val readAllClicked = _readAllClicked.asStateFlow()

    // 현재 사용자의 UID를 가져오되, 로그인이 안 된 경우를 대비해 null 허용으로 체크
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId: String get() = auth.currentUser?.uid ?: ""

    init {
        if (currentUserId.isNotEmpty()) {
            observeIncomingNotification()
        }
    }

    // 실시간 구독 — 새 알림 수신 시 목록 상단에 추가
    fun observeIncomingNotification() {
        viewModelScope.launch {
            notificationRepository.observeNotificationList(currentUserId)
                .collectLatest { liveNotifications ->
                    _notification.value = liveNotifications
                    Log.d("NotificationVM", "🔥 [실시간 동기화 완료] 현재 알림 개수: ${liveNotifications.size}개")
                }
        }
    }

    // 리스트에서 완전히 삭제 (스와이프 시) + Cloud Functions 연동
    fun markAsReadAndDelete(id: String) {
        // Cloud Functions 비동기 삭제
        viewModelScope.launch {
            notificationRepository.deleteNotifications(currentUserId, id)
        }
    }

    // 단순 읽음 처리 (DM 클릭, 무전 클릭 시)
    fun markAsRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markNotificationsAsRead("single", id)
        }
    }

    // 친구 요청 수락 클릭
    fun onAcceptFriendClick(id: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotifications(currentUserId, id)
        }
    }

    // 친구 요청 거절 클릭
    fun onRejectFriendClick(id: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotifications(currentUserId, id)
        }
    }

    // 전체 확인: 보라색 점만 제거 (isRead는 건드리지 않음)
    fun onReadAllClick() {
        _readAllClicked.value = true

        viewModelScope.launch {
            notificationRepository.markNotificationsAsRead("all", null)
        }
    }
}