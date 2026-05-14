package com.bbip.bbipit.presentation.noti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.domain.entity.Notifications
import com.bbip.bbipit.domain.usecase.GetNotiListUseCase
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class NotiViewModel @Inject constructor(
    private val getNotiListUseCase: GetNotiListUseCase
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notifications>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _topBannerNoti = MutableStateFlow<Notifications?>(null)
    val topBannerNoti = _topBannerNoti.asStateFlow()

    init {
        fetchNotifications()
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _notifications.value = listOf(
                Notifications(
                    notiId = "1",
                    type = "WALKIE",
                    senderName = "Alex Rivera",
                    content = "",
                    createdAt = Timestamp(Date(now - 120000)),
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
            ))
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

    // 친구 수락 클릭 (읽음 처리 + 내용 변경)
    fun onAcceptFriendClick(notiId: String) {
        _notifications.value = _notifications.value.map {
            if (it.notiId == notiId) it.copy(isRead = true, content = "수락됨") else it
        }
    }

    // 친구 거절 클릭 (읽음 처리 + 내용 변경)
    fun onRejectFriendClick(notiId: String) {
        _notifications.value = _notifications.value.map {
            if (it.notiId == notiId) it.copy(isRead = true, content = "거절됨") else it
        }
    }
    // 전체 확인 시 읽음 상태로만 변경 (보라색 동그라미 제거)
    fun onReadAllClick() {
        _notifications.value = _notifications.value.map {
            it.copy(isRead = true)
        }
    }

    fun dismissBanner() { _topBannerNoti.value = null }
}
