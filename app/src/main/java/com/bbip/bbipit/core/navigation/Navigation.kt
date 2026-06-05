package com.bbip.bbipit.core.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.presentation.auth.ui.SignInScreen
import com.bbip.bbipit.presentation.auth.ui.SignUpScreen
import com.bbip.bbipit.presentation.chat.ui.ChatDetailScreen
import com.bbip.bbipit.presentation.chat.ui.ChatListScreen
import com.bbip.bbipit.presentation.friendship.ui.FriendListScreen
import com.bbip.bbipit.presentation.friendship.ui.FriendRequestScreen
import com.bbip.bbipit.presentation.map.ui.MapScreen
import com.bbip.bbipit.presentation.mypage.EditProfileScreen
import com.bbip.bbipit.presentation.mypage.MyPageScreen
import com.bbip.bbipit.presentation.notification.ui.NotificationScreen
import com.bbip.bbipit.presentation.notification.viewmodel.NotificationViewModel
import com.bbip.bbipit.presentation.permission.ServiceRestrictedScreen
import kotlinx.coroutines.delay

@Composable
fun BBipItNavigation(
    navController: NavHostController,
    authRepository: AuthRepository,
    notificationViewModel: NotificationViewModel,
    notificationIntent: Intent? = null
) {

    val isLogin = authRepository.isAutoLogin()

    // 로그인은 되어있으나 이메일 인증이 완료되지 않은 유저라면 자동 로그인을 차단합니다.
    val isEmailVerified = authRepository.isEmailVerified()

    // 두 조건이 모두 만족해야만 홈 화면(Map)으로 바로 진입합니다.
    val notificationType = notificationIntent?.getStringExtra("notification_type")

    val start = when {
        !(isLogin && isEmailVerified) -> Routes.SignIn

        notificationType == "WALKIE" -> Routes.Map
        notificationType != null -> Routes.Notification

        else -> Routes.Map
    }

    // 만약 로그인은 되어있는데 이메일 인증이 안 된 유저가 앱을 켰다면,
    // 안전하게 기기 세션을 한번 더 로그아웃 시켜줍니다.
    if (isLogin && !isEmailVerified) {
        LaunchedEffect(Unit) {
            authRepository.signOut()
        }
    }

    NavHost(
        navController = navController,
        startDestination = start
    ){
        composable<Routes.SignIn> { SignInScreen(navController) }
        composable<Routes.SignUp> { SignUpScreen(navController) }
        composable<Routes.Map> { MapScreen(navController) }
        composable<Routes.MyPage> { MyPageScreen(navController) }
        composable<Routes.ChatList> { ChatListScreen(navController) }
        composable<Routes.Notification> { NotificationScreen(navController, notificationViewModel)}
        composable<Routes.ChatRoom> { ChatDetailScreen(navController) }
        composable<Routes.EditProfile> { EditProfileScreen(navController) }
        composable<Routes.FriendList> { FriendListScreen(navController) }
        composable<Routes.FriendRequestList> { FriendRequestScreen(navController) }
        composable<Routes.ServiceRestricted> { ServiceRestrictedScreen(navController) }
    }

    LaunchedEffect(notificationIntent) {
        if (!(isLogin && isEmailVerified)) return@LaunchedEffect
        val type = notificationIntent?.getStringExtra("notification_type") ?: return@LaunchedEffect

        val notificationId = notificationIntent.getStringExtra("notification_id") ?: ""
        val roomId = notificationIntent.getStringExtra("notification_room_id") ?: ""

        if (notificationId.isNotEmpty()) {
            notificationViewModel.markAsRead(notificationId)
        }

        when (type) {
            "DM" -> {
                if (roomId.isNotEmpty()) {
                    val receiverId =
                        notificationIntent.getStringExtra("notification_receiver_id") ?: ""
                    while (navController.currentBackStackEntry?.destination?.route?.contains("Notification") != true) {
                        delay(50)
                    }
                    navController.navigate(
                        Routes.ChatRoom(roomId = roomId, receiverId = receiverId)
                    )
                }
            }

            "REQ" -> {
                if (notificationId.isNotEmpty()) notificationViewModel.markAsRead(notificationId)
                while (navController.currentBackStackEntry?.destination?.route?.contains("Notification") != true) {
                    delay(50)
                }
                navController.navigate(Routes.FriendRequestList)
            }

            "ACP" -> {
                if (notificationId.isNotEmpty()) notificationViewModel.markAsRead(notificationId)
                while (navController.currentBackStackEntry?.destination?.route?.contains("Notification") != true) {
                    delay(50)
                }
                navController.navigate(Routes.FriendList)
            }
        }
    }
}
