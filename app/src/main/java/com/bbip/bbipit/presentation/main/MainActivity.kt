package com.bbip.bbipit.presentation.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bbip.bbipit.presentation.base.BottomBar
import com.bbip.bbipit.core.navigation.BBipItNavigation
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.BbipitTheme
import com.bbip.bbipit.domain.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        var keyHash = Utility.getKeyHash(this)
//        Log.d("keyHash", keyHash)
        setContent {
            BbipitTheme {
                Surface(
                    modifier = Modifier.Companion.fillMaxSize(),
                    contentColor = Color.Companion.Transparent,
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()

                    val showBottomBar = navBackStackEntry?.destination?.let { destination ->
                        destination.hasRoute<Routes.Map>() ||
                                destination.hasRoute<Routes.ChatList>() ||
                                destination.hasRoute<Routes.MyPage>() || destination.hasRoute<Routes.Noti>()

                    } ?: false

                    Scaffold(
                        modifier = Modifier.Companion.fillMaxSize(),
                        bottomBar = { if (showBottomBar) BottomBar(navController) }
                    ) { _ ->
                        Box(modifier = Modifier.Companion.fillMaxSize()) {
                            BBipItNavigation(
                                navController = navController,
                                userRepository = userRepository
                            )
                        }
                    }

                }
            }
        }
    }

}