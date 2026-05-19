package com.bbip.bbipit.presentation.notification

import android.widget.Toast
import android.util.Log
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
    val showBanner by viewModel.showInAppBanner.collectAsState()
    val bannerNoti by viewModel.latestInAppNotification.collectAsState()
    val context = LocalContext.current

    if (showBanner && bannerNoti != null && bannerNoti!!.type != "WALKIE") {
        NotificationBanner(
            item = bannerNoti!!,
            modifier = modifier,
            onDismiss = { viewModel.dismissBanner() },
            onClick = {
                viewModel.markAsRead(bannerNoti!!.id)

                // 알림 타입별 기능 분기 처리
                when (bannerNoti!!.type) {
                    // 1. DM: 해당 채팅방으로 이동
                    "DM" -> {
                        if (!bannerNoti!!.roomId.isNullOrEmpty()) {
                            try {
                                navController.navigate(Routes.ChatRoom(roomId = bannerNoti!!.roomId)) {
                                    launchSingleTop = true // 화면 중복 쌓임 방지
                                }
                            } catch (e: Exception) {
                                Log.e("NotificationBannerHost", "DM 채팅방 이동 크래시 방지: ${e.message}")
                            }
                        }
                    }

                    // 2. 친구 요청: 안내 토스트 출력(임시)
                    "REQ" -> {
                        try {
                            Toast.makeText(
                                context,
                                "친구화면으로 이동합니다",
                                Toast.LENGTH_SHORT
                            ).show()
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