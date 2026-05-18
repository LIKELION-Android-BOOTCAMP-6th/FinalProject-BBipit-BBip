package com.bbip.bbipit.presentation.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bbip.bbipit.presentation.base.BottomBar
import com.bbip.bbipit.core.navigation.BBipItNavigation
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.BbipitTheme
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.presentation.base.VoicePlayerViewModel
import com.bbip.bbipit.presentation.base.VoiceReceptionOverlay
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.collectAsState
import com.bbip.bbipit.presentation.chat.viewmodel.ChatListViewModel

// 파이어베이스 App Check 관련 임포트 추가
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // [App Check 디버그 설정 추가]
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        setContent {
            val voicePlayerViewModel: VoicePlayerViewModel = hiltViewModel()
            val chatListViewModel: ChatListViewModel = hiltViewModel()

            BbipitTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                val chatUiState by chatListViewModel.uiState.collectAsState()

                // 💡 3. 내 저금통(unreadCount)이 0보다 큰 방이 단 하나라도 있는지 실시간 팩트 체크!
                val hasUnreadChat = chatUiState.chatList.any { it.unreadCount > 0 }

                val showBottomBar = navBackStackEntry?.destination?.let { destination ->
                    destination.hasRoute<Routes.Map>() ||
                            destination.hasRoute<Routes.ChatList>() ||
                            destination.hasRoute<Routes.MyPage>() || destination.hasRoute<Routes.Notification>()

                } ?: false

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { if (showBottomBar) BottomBar(navController, hasUnreadChat = hasUnreadChat) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        BBipItNavigation(
                            navController = navController,
                            authRepository = authRepository
                        )

                        // 전역 음성 수신 오버레이 (바텀 네비게이션 위에 배치)
                        VoiceReceptionOverlay(
                            viewModel = voicePlayerViewModel,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

}