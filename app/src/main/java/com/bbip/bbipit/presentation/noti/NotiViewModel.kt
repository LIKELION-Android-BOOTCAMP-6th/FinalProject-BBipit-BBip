package com.bbip.bbipit.presentation.noti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.domain.entity.NotiItem
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

    private val _notifications = MutableStateFlow<List<NotiItem>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _topBannerNoti = MutableStateFlow<NotiItem?>(null)
    val topBannerNoti = _topBannerNoti.asStateFlow()

    init {
        fetchNotifications()
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _notifications.value = listOf(
                NotiItem(
                    id = "1",
                    type = "WALKIE",
                    senderName = "Alex Rivera",
                    content = "",
                    createdAt = Timestamp(Date(now - 120000)),
                ),
                NotiItem(
                    id = "2",
                    type = "DM",
                    senderName = "박미나",
                    content = "오늘 저녁 어때?",
                    roomId = "room_123",
                    createdAt = Timestamp(Date(now - 600000))
                ),
                NotiItem(
                    id = "3",
                    type = "REQ",
                    senderName = "Jordan",
                    createdAt = Timestamp(Date(now - 1800000))
                ),
            NotiItem(
                id = "noti_2",
                type = "WALKIE",
                senderName = "김민성",
                createdAt = Timestamp(Date(now - (5 * 60 * 60 * 1000L)))
            ))
        }
    }

    fun markAsReadAndDelete(notiId: String) {
        val currentList = _notifications.value.toMutableList()
        if (currentList.removeAll { it.id == notiId }) {
            _notifications.value = currentList.toList()
        }
    }

    fun onAcceptFriendClick(notiId: String) = markAsReadAndDelete(notiId)
    fun onRejectFriendClick(notiId: String) = markAsReadAndDelete(notiId)
    fun onReadAllClick() { _notifications.value = emptyList() }
    fun dismissBanner() { _topBannerNoti.value = null }
}