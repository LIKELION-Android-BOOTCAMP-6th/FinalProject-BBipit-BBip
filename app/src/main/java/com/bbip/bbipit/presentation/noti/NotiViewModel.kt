// presentation/noti/NotiViewModel.kt
package com.bbip.bbipit.presentation.noti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.domain.entity.Notifications
import com.bbip.bbipit.domain.repository.NotiRepository
import com.bbip.bbipit.domain.usecase.GetNotiListUseCase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class NotiViewModel @Inject constructor(
    private val getNotiListUseCase: GetNotiListUseCase,
    private val notiRepository: NotiRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notifications>>(emptyList())
    val notifications = _notifications.asStateFlow()

    // 전체 확인 버튼 클릭 여부 (보라색 점 제거용, isRead와 무관)
    private val _readAllClicked = MutableStateFlow(false)
    val readAllClicked = _readAllClicked.asStateFlow()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // 구독 해제용 리스너
    private var notiListener: ListenerRegistration? = null

    init {
        fetchNotifications()
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            // 새 알림 불러올 때 전체 확인 상태 초기화
            _readAllClicked.value = false

            // TODO: 실제 연동 시 아래 주석 해제 후 더미 데이터 삭제
            // getNotiListUseCase(currentUserId)
            //     .onSuccess { _notifications.value = it }
            //     .onFailure { }

            val now = System.currentTimeMillis()
            _notifications.value = listOf(
                Notifications(
                    notiId = "1",
                    type = "WALKIE",
                    senderName = "Alex Rivera",
                    createdAt = Timestamp(Date(now - 120000))
                ),
                Notifications(
                    notiId = "2",
                    type = "DM",
                    senderName = "박미나",
                    content = "오늘 저녁 어때?",
                    roomId = "room_123",
                    createdAt = Timestamp(Date(now - 600000))
                ),
                Notifications(
                    notiId = "3",
                    type = "REQ",
                    senderName = "Jordan",
                    createdAt = Timestamp(Date(now - 1800000))
                ),
                Notifications(
                    notiId = "noti_2",
                    type = "WALKIE",
                    senderName = "김민성",
                    createdAt = Timestamp(Date(now - (5 * 60 * 60 * 1000L)))
                )
            )
        }
    }

    // 리스트에서 완전히 삭제 (스와이프 시)
    fun markAsReadAndDelete(notiId: String) {
        _notifications.value = _notifications.value.filter { it.notiId != notiId }
    }

    // 단순 읽음 처리 (DM 클릭 시 등)
    fun markAsRead(notiId: String) {
        _notifications.value = _notifications.value.map {
            if (it.notiId == notiId) it.copy(isRead = true) else it
        }
    }

    // 친구 수락 클릭 (isRead = true + 배지용 content 변경)
    fun onAcceptFriendClick(notiId: String) {
        _notifications.value = _notifications.value.map {
            if (it.notiId == notiId) it.copy(isRead = true, content = "수락됨") else it
        }
    }

    // 친구 거절 클릭 (isRead = true + 배지용 content 변경)
    fun onRejectFriendClick(notiId: String) {
        _notifications.value = _notifications.value.map {
            if (it.notiId == notiId) it.copy(isRead = true, content = "거절됨") else it
        }
    }

    // 전체 확인: 보라색 점만 제거 (isRead는 건드리지 않음)
    fun onReadAllClick() {
        _readAllClicked.value = true
    }

    // ViewModel 소멸 시 리스너 해제
    override fun onCleared() {
        super.onCleared()
        notiListener?.remove()
    }
}