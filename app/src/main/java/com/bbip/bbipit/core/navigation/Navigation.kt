package com.bbip.bbipit.core.navigation

import androidx.compose.runtime.Composable
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
import com.bbip.bbipit.presentation.notification.NotificationScreen
import com.bbip.bbipit.presentation.notification.NotificationViewModel

@Composable
fun BBipItNavigation(
    navController: NavHostController,
    authRepository: AuthRepository,
    notificationViewModel: NotificationViewModel
){

    val isLogin = authRepository.isAutoLogin()
    val start = if (isLogin) Routes.Map else Routes.SignIn

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
    }
}
