package com.bbip.bbipit.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.presentation.auth.ui.SignInScreen
import com.bbip.bbipit.presentation.auth.ui.SignUpScreen
import com.bbip.bbipit.presentation.chat.ui.ChatDetailScreen
import com.bbip.bbipit.presentation.chat.ui.ChatListScreen
import com.bbip.bbipit.presentation.map.ui.MapScreen
import com.bbip.bbipit.presentation.noti.NotiScreen
import com.bbip.bbipit.presentation.mypage.MyPageScreen

@Composable
fun BBipItNavigation(
    navController: NavHostController,
    userRepository: UserRepository
){

    val isLogin = userRepository.isLogin()
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
        composable<Routes.Noti> { NotiScreen(navController) }
        composable<Routes.ChatRoom> { ChatDetailScreen(navController) }

    }
}