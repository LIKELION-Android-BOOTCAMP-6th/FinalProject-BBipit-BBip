package com.bbip.bbipit.presentation.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.bottomBarBack
import com.bbip.bbipit.core.ui.theme.primary

@Composable
fun BottomBar(navController: NavController){
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        ,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = bottomBarBack.copy(alpha = 0.4f),
//        contentColor = contentColorFor(bottomBarBack),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(68.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically) {

            val isMapSelected = currentDestination?.hasRoute<Routes.Map>() == true
            IconButton(
                onClick = {
                    if (!isMapSelected){
                        navController.navigate(Routes.Map){
                            popUpTo(Routes.Map){ inclusive = false}
                            launchSingleTop = true
                        }
                    }
                },
            ) {
                Icon(imageVector = if(isMapSelected) Icons.Default.Home else Icons.Outlined.Home,
                    contentDescription = "홈",
                    tint = if (isMapSelected) primary else Color.Gray
                )
            }

            val isChatSelected = currentDestination?.hasRoute<Routes.ChatList>() == true
            IconButton(
                onClick = {
                    if (!isChatSelected){
                        navController.navigate(Routes.ChatList){
                            popUpTo(Routes.Map){ inclusive = false}
                            launchSingleTop = true
                        }
                    }
                },
            ) {
                Icon(imageVector = if(isChatSelected) Icons.AutoMirrored.Filled.Chat else Icons.Default.ChatBubbleOutline,
                    contentDescription = "채팅",
                    tint = if (isChatSelected) primary else Color.Gray
                )
            }

            val isNotiSelected = currentDestination?.hasRoute<Routes.Noti>() == true
            IconButton(
                onClick = {
                    if (!isNotiSelected){
                        navController.navigate(Routes.Noti){
                            popUpTo(Routes.Noti){ inclusive = false}
                            launchSingleTop = true
                        }
                    }
                },
            ) {
                Icon(imageVector = if(isNotiSelected) Icons.Default.Notifications else Icons.Outlined.Notifications,
                    contentDescription = "알림",
                    tint = if (isNotiSelected) primary else Color.Gray
                )
            }
            val isMyPageSelected = currentDestination?.hasRoute<Routes.MyPage>() == true
            IconButton(
                onClick = {
                    if (!isMyPageSelected){
                        navController.navigate(Routes.MyPage){
                            popUpTo(Routes.Map){ inclusive = false}
                            launchSingleTop = true
                        }
                    }
                },
            ) {
                Icon(imageVector = if(isMyPageSelected) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle,
                    contentDescription = "마이페이지",
                    tint = if (isMyPageSelected) primary else Color.Gray
                )
            }
        }


    }
}