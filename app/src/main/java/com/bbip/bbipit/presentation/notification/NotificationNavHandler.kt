/*
package com.bbip.bbipit.presentation.notification

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bbip.bbipit.core.navigation.Routes

*/
/**
 * 알림 클릭 Intent 감지 및 타입별 화면 이동 처리 컴포저블
 * MainActivity의 pendingIntent를 구독하여 navController 준비 후 자동 처리
 *//*


@Composable
fun GoToScreenByNotification(
    pendingIntent: Intent?,
    navController: NavHostController,
    context: Context,
    onHandled: () -> Unit
) {
    if (pendingIntent == null) return

    val type = pendingIntent.getStringExtra("notification_type") ?: return
    val roomId = pendingIntent.getStringExtra("notification_room_id") ?: ""

    LaunchedEffect(Unit) {
        // NavHost startDestination 세팅 완료까지 대기
        while (navController.currentBackStackEntry == null) {
            kotlinx.coroutines.delay(50)
        }
        // startDestination 완전히 안착할 때까지 추가 대기
        kotlinx.coroutines.delay(100)

        when (type) {
            // 채팅방 화면으로 이동 (roomId 필수)
            "DM" -> {
                if (roomId.isNotEmpty()) {
                    navController.navigate(Routes.ChatRoom(roomId = roomId)) {
                        launchSingleTop = true
                    }
                }
            }
            // 친구 요청 화면으로 이동
            "REQ" -> {
                navController.navigate(Routes.FriendRequestList) {
                    launchSingleTop = true
                }
            }
            // 무전은 토스트로 안내
            "WALKIE" -> {
                Toast.makeText(context, "무전 알림을 확인하세요.", Toast.LENGTH_SHORT).show()
            }
        }
        onHandled()
    }
}*/
