package com.bbip.bbipit.presentation.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.bbip.bbipit.core.navigation.Routes

@Composable
fun NotificationBannerHost(
    viewModel: NotificationViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val showBanner by viewModel.showInAppBanner.collectAsState()
    val bannerNoti by viewModel.latestInAppNotification.collectAsState()

    if (showBanner && bannerNoti != null) {
        NotificationBanner(
            item = bannerNoti!!,
            modifier = modifier,
            onDismiss = { viewModel.dismissBanner() },
            onClick = {
                viewModel.markAsRead(bannerNoti!!.id)
                if (bannerNoti!!.type == "DM") {
                    navController.navigate(Routes.ChatRoom(roomId = bannerNoti!!.roomId))
                }
                viewModel.dismissBanner()
            }
        )
    }
}