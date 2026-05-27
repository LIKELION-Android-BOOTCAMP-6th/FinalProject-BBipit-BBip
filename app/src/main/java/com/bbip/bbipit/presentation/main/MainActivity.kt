package com.bbip.bbipit.presentation.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bbip.bbipit.presentation.base.BottomBar
import com.bbip.bbipit.core.navigation.BBipItNavigation
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.BbipitTheme
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.presentation.base.VoicePlayerViewModel
import com.bbip.bbipit.presentation.base.VoicePlayerScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ProcessLifecycleOwner
import com.bbip.bbipit.core.base.AppLifecycleObserver
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.presentation.chat.viewmodel.ChatListViewModel
import com.bbip.bbipit.presentation.notification.viewmodel.NotificationViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

// 파이어베이스 App Check 관련 임포트 추가
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val bottomBarViewModel: BottomBarViewModel by viewModels()
    @Inject
    lateinit var authRepository: AuthRepository
    @Inject
    lateinit var liveStatusRepository: LiveStatusRepository
    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    // 알림 클릭 시 이동 처리를 위한 반응형 상태
    private var pendingNotificationIntent by mutableStateOf<Intent?>(null)

    private var pendingNotificationId by mutableStateOf<String?>(null)

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationIntent = intent
        pendingNotificationId = intent.getStringExtra("notification_id")

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // App Check 디버그 환경 구성 설정
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // 앱 수명 주기 관찰자 등록
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        // 알림 클릭으로 온 Intent인지 구분
        pendingNotificationIntent = if (intent.hasExtra("notification_type")) intent else null
        pendingNotificationId = intent.getStringExtra("notification_id")

        setContent {

            val voicePlayerViewModel: VoicePlayerViewModel = hiltViewModel()
            val chatListViewModel: ChatListViewModel = hiltViewModel()
            val notificationViewModel: NotificationViewModel = hiltViewModel()

            val isShownDrawer by bottomBarViewModel.isDrawerShown.collectAsState()
            BbipitTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                val chatUiState by chatListViewModel.uiState.collectAsState()

                // 미독 알림 개수(unreadCount) 존재 여부 실시간 확인 플래그
                val hasUnreadChat = chatUiState.chatList.any { it.unreadCount > 0 }

                // 바텀바 노출 여부 설정
                val isMainRoute = navBackStackEntry?.destination?.let { destination ->
                    destination.hasRoute<Routes.Map>() ||
                            destination.hasRoute<Routes.ChatList>() || destination.hasRoute<Routes.FriendList>() ||
                            destination.hasRoute<Routes.MyPage>() || destination.hasRoute<Routes.Notification>()

                } ?: false

                val showBottomBar = isMainRoute && !isShownDrawer
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
//                        if (showBottomBar) BottomBar(navController, hasUnreadChat = hasUnreadChat)
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                        ) {
                            BottomBar(navController, hasUnreadChat = hasUnreadChat)
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
//                            .padding(innerPadding)
                    ) {
                        // 배너 클릭 진입 시 알림 처리
                        LaunchedEffect(pendingNotificationId) {
                            pendingNotificationId?.let { id ->
                                val type = pendingNotificationIntent?.getStringExtra("notification_type")
                                if (type == "WALKIE") {
                                    notificationViewModel.playWalkie(pendingNotificationIntent!!)
                                } else {
                                    notificationViewModel.markAsRead(id)
                                }
                                pendingNotificationId = null
                            }
                        }
                        BBipItNavigation(
                            navController = navController,
                            authRepository = authRepository,
                            notificationViewModel = notificationViewModel,
                            notificationIntent = pendingNotificationIntent
                        )

                        // 음성 수신 오버레이 패널 (바텀바 유무에 따라 하단 여백 조절)
                        VoicePlayerScreen(
                            viewModel = voicePlayerViewModel,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(
                                    bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 100.dp
                                )
                        )
                    }
                }
            }
        }
    }
}