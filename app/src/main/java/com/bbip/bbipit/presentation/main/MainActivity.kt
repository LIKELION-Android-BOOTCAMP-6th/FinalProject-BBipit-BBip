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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.bbip.bbipit.presentation.notification.NotificationViewModel

// 파이어베이스 App Check 관련 임포트 추가
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authRepository: AuthRepository
    @Inject
    lateinit var liveStatusRepository: LiveStatusRepository
    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    override fun onDestroy() {
        // 앱 프로세스 파괴 직전 내 실시간 상태 오프라인 변경 및 서버 동기화 처리
        val myUid = authRepository.getCurrentUserUid()
        val currentStatus = liveStatusRepository.getCachedMyLiveStatus()

        if (myUid != null && currentStatus != null) {
            // suspend 함수의 완전한 완료 대기 보장 목적의 runBlocking 구문
            kotlinx.coroutines.runBlocking {
                liveStatusRepository.updateMyLiveStatus(
                    currentStatus.copy(
                        isOnline = false,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // App Check 디버그 환경 구성 설정
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // 앱 전체 프로세스 수명 주기(ProcessLifecycleOwner) 대상 관찰자 등록
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        setContent {
            val voicePlayerViewModel: VoicePlayerViewModel = hiltViewModel()
            val chatListViewModel: ChatListViewModel = hiltViewModel()
            val notificationViewModel: NotificationViewModel = hiltViewModel()

            BbipitTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                val chatUiState by chatListViewModel.uiState.collectAsState()

                // 미독 알림 개수(unreadCount) 존재 여부 실시간 확인 플래그
                val hasUnreadChat = chatUiState.chatList.any { it.unreadCount > 0 }

                val showBottomBar = navBackStackEntry?.destination?.let { destination ->
                    destination.hasRoute<Routes.Map>() ||
                            destination.hasRoute<Routes.ChatList>() || destination.hasRoute<Routes.FriendList>() ||
                            destination.hasRoute<Routes.MyPage>() || destination.hasRoute<Routes.Notification>()

                } ?: false

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { if (showBottomBar) BottomBar(navController, hasUnreadChat = hasUnreadChat) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
//                            .padding(innerPadding)
                    ) {
                        BBipItNavigation(
                            navController = navController,
                            authRepository = authRepository,
                            notificationViewModel = notificationViewModel
                        )

                        // 전역 음성 수신 오버레이 패널 (바텀 네비게이션 상단 배치)
                        VoicePlayerScreen(
                            viewModel = voicePlayerViewModel,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(
                                    // 바텀바가 표시 중일 때만 바텀바의 높이만큼 하단 여백
                                    bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 100.dp
                                )
                        )
                    }
                }
            }
        }
    }
}