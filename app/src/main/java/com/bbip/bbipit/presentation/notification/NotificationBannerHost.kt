/*
package com.bbip.bbipit.presentation.notification

import android.widget.Toast
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.bbip.bbipit.core.navigation.Routes

@Composable
fun NotificationBannerHost(
    viewModel: NotificationViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val banner = bannerNotification

    AnimatedVisibility(
        visible = showBanner && banner != null && banner.type != "WALKIE",
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(durationMillis = 350)
        ),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = modifier
    ) {
        if (banner != null) {
            NotificationBanner(
                item = banner,
                onDismiss = { viewModel.dismissBanner() },
                onClick = {
                    viewModel.markAsRead(banner.id)

                    when (banner.type) {
                        "DM" -> {
                            if (!banner.roomId.isNullOrEmpty()) {
                                try {
                                    navController.navigate(Routes.ChatRoom(roomId = banner.roomId)) {
                                        launchSingleTop = true
                                    }
                                } catch (e: Exception) {
                                    Log.e("NotificationBannerHost", "DM 채팅방 이동 크래시 방지: ${e.message}")
                                }
                            }
                        }
                        "REQ" -> {
                            try {
                                Toast.makeText(context, "친구화면으로 이동합니다", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("NotificationBannerHost", "친구화면 이동 크래시 방지: ${e.message}")
                            }
                        }
                    }

                    viewModel.dismissBanner()
                }
            )
        }
    }
}*/
