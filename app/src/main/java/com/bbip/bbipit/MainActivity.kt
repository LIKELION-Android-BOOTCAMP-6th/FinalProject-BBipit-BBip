package com.bbip.bbipit

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bbip.bbipit.core.component.BackgroundBox
import com.bbip.bbipit.core.navigation.BBipItNavigation
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.BbipitTheme
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.domain.repository.UserRepository

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        var keyHash = Utility.getKeyHash(this)
//        Log.d("keyHash", keyHash)
        setContent {
            val navController = rememberNavController()
//            val navController = rememberNavController()
//            BBipItNavigation(
//                navController = navController,
//                userRepository = userRepository
//            )
            BbipitTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        BBipItNavigation(
                            navController = navController,
                            userRepository = userRepository
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text("채팅", modifier = Modifier.weight(1f).clickable {
                            navController.navigate(Routes.ChatList)
                        })

                        Text("홈", modifier = Modifier.weight(1f).clickable {
                            navController.navigate(Routes.Map)
                        })

                        Text("마이페이지", modifier = Modifier.weight(1f).clickable {
                            navController.navigate(Routes.MyPage)
                        })
                    }
                }
            }
        }
    }

}



