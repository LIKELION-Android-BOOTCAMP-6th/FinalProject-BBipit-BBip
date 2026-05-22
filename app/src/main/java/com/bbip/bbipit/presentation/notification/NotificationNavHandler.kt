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

/**
 * 알림 클릭 Intent 감지 및 타입별 화면 이동 처리 컴포저블
 * MainActivity의 pendingIntent를 구독하여 navController 준비 후 자동 처리
 */
@Composable
fun GoToScreenByNotification(
    pendingIntent: Intent?,
    navController: NavHostController,
    context: Context,
    onHandled: () -> Unit
) {
    // navController의 현재 상태 관찰 (NavHost 준비 여부 확인용)
    val currentEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(pendingIntent, currentEntry) {
        val intent = pendingIntent ?: return@LaunchedEffect
        // NavHost가 첫 화면(StartDestination)을 세팅하기 전에는 이동을 보류합니다.
        if (currentEntry == null) return@LaunchedEffect

        val type = intent.getStringExtra("notification_type")
        val roomId = intent.getStringExtra("notification_room_id") ?: ""

        Log.d("NotificationNav", "알림 클릭 감지 - 타입: $type, 경로: ${currentEntry?.destination?.route}")

        when (type) {
            "DM" -> {
                if (roomId.isNotEmpty()) {
                    navController.navigate(Routes.ChatRoom(roomId = roomId)) {
                        launchSingleTop = true
                    }
                }
            }
            "REQ" -> {
                navController.navigate(Routes.FriendRequestList) {
                    // 동일 화면 중복 생성 방지 및 부드러운 전환을 위해 설정
                    launchSingleTop = true
                    // 필요한 경우 백스택을 정리하여 뒤로가기 시 지도로 가도록 설정 가능
                }
            }
            // 무전은 토스트로 안내
            "WALKIE" -> {
                Toast.makeText(context, "무전 알림을 확인하세요.", Toast.LENGTH_SHORT).show()
            }
        }
        onHandled()
    }
}