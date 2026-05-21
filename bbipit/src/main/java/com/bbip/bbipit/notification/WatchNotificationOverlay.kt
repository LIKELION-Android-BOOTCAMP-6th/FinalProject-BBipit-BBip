package com.bbip.bbipit.notification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WatchNotificationOverlay(
    viewModel: WatchNotificationViewModel,
    onBannerClick: (Notification) -> Unit
) {
    val notifications by viewModel.notification.collectAsState()
    var showBanner by remember { mutableStateOf(false) }
    var currentItem by remember { mutableStateOf<Notification?>(null) }

    // 파이어스토어 관찰 시작
    LaunchedEffect(Unit) {
        viewModel.startObserving()
    }

    // 신규 추가 알림 데이터 수신 및 배너 상태 변경
    LaunchedEffect(Unit) {
        viewModel.bannerEvent.collectLatest { newNotification ->
            currentItem = newNotification
            showBanner = true
        }
    }

    // 알림 전체 삭제 시 배너 종료 예외 처리
    LaunchedEffect(notifications) {
        if (notifications.isEmpty()) {
            showBanner = false
        }
    }

    if (showBanner && currentItem != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            WatchNotificationBanner(
                item = currentItem!!,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                onDismiss = { showBanner = false },
                onClick = {
                    showBanner = false
                    viewModel.markAsRead(currentItem!!.id)
                    onBannerClick(currentItem!!)
                }
            )
        }
    }
}