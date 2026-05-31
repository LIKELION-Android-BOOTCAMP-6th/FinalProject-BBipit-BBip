package com.bbip.bbipit.presentation.main

import android.app.Activity
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.presentation.chat.viewmodel.ChatListViewModel
import com.bbip.bbipit.presentation.notification.viewmodel.NotificationViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.Lifecycle
import com.bbip.bbipit.core.base.LifeCycleManager

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
    lateinit var lifeCycleManager: LifeCycleManager

    // 알림 클릭 시 이동 처리를 위한 반응형 상태
    private var pendingNotificationIntent by mutableStateOf<Intent?>(null)

    private var pendingNotificationId by mutableStateOf<String?>(null)

    private val TAG = "MobileMainActivity"

    // 안드로이드 공식 권한 요청 런처 정의
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isBluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            true
        }

        if (isBluetoothGranted) {
            Log.d(TAG, "✅ 사용자가 블루투스 연결 권한을 승인했습니다.")
            // 필요 시 여기에 워치로 다시 READY 신호를 강제 푸시하는 로직을 연동할 수 있습니다.
        } else {
            Log.w(TAG, "❌ 사용자가 블루투스 권한을 거부했습니다.")
        }
    }

    /**
     * 서비스로부터 온 인텐트를 분석하여 필요시 시스템 권한 팝업 가동
     */
    private fun checkIntentAndRequestPermissions(intent: Intent?) {
        val shouldRequest = intent?.getBooleanExtra("ACTION_REQUEST_PERMISSIONS", false) ?: false

        if (shouldRequest) {
            Log.d(TAG, "🚀 서비스 요청 수신: 유저에게 즉시 권한 승인 팝업을 표시합니다.")

            // 안드로이드 12(API 31) 이상일 때만 블루투스 커넥트 권한이 필수입니다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationIntent = intent
        pendingNotificationId = intent.getStringExtra("notification_id")

        // 처음 앱이 켜질 때 서비스로부터 전달받은 인텐트가 있는지 검사
        checkIntentAndRequestPermissions(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 처음 앱이 켜질 때 서비스로부터 전달받은 인텐트가 있는지 검사
        checkIntentAndRequestPermissions(intent)

        // App Check 디버그 환경 구성 설정
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // 앱 수명 주기 관찰자 등록
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifeCycleManager)

        // 알림 클릭으로 온 Intent인지 구분
        pendingNotificationIntent = if (intent.hasExtra("notification_type")) intent else null
        pendingNotificationId = intent.getStringExtra("notification_id")
        Log.d("MainActivity", "onCreate - type: ${intent.getStringExtra("notification_type")}, id: ${intent.getStringExtra("notification_id")}")

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

                val context = LocalContext.current
                var initialCheckStage by rememberSaveable { mutableStateOf(0) }
                val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()

                LaunchedEffect(navBackStackEntry, lifecycleState) {
                    val currentDestination = navBackStackEntry?.destination ?: return@LaunchedEffect

                    // 앱이 최종적으로 활성화 상태일 때만 체크
                    if (lifecycleState == Lifecycle.State.RESUMED) {

                        if (initialCheckStage == 0) {
                            if (currentDestination.hasRoute<Routes.Map>()) {
                                initialCheckStage = 1 // 맵 화면에 도달했음을 기록
                            }
                            Log.d("MainActivity", "ℹ️ 앱 최초 렌더링 단계: MapScreen의 팝업 권한 처리를 위해 전역 체크를 스킵합니다.")
                            return@LaunchedEffect
                        }

                        if (initialCheckStage == 1) {
                            // 맵 화면에 아직 머물러 있고, 아직 사용자가 시스템 팝업 결과를 내지 않은 상태라면 계속 스킵
                            if (currentDestination.hasRoute<Routes.Map>()) {
                                Log.d("MainActivity", "ℹ️ 최초 권한 요청 대기 단계: 사용자의 입력을 기다립니다.")
                                return@LaunchedEffect
                            } else {
                                // 사용자가 거부하여 ServiceRestricted으로 이동하기 시작한 시점부터 전역 감지를 활성화
                                initialCheckStage = 2
                            }
                        }

                        // 위치 권한 체크
                        val hasLocation = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        // 알림 권한 체크
                        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                        // 음성 권한 체크
                        val hasAudio = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        val isRestrictedScreen = currentDestination.hasRoute<Routes.ServiceRestricted>()

                        // 모든 권한이 허용된 경우 -> 정상 지도 화면으로 복구
                        if (hasLocation && hasNotification && hasAudio) {
                            if (isRestrictedScreen) {
                                Log.d("MainActivity", "✅ 권한 허용 감지 -> MapScreen 복귀")
                                navController.navigate(Routes.Map) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                        // 권한이 거부된 경우 -> 이용 제한 화면으로 강제 이동
                        else {
                            if (!isRestrictedScreen) {
                                Log.d("MainActivity", "🚨 권한 영구 거부 감지 -> ServiceRestrictedScreen 강제 이동")
                                navController.navigate(Routes.ServiceRestricted) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }

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
                            Log.d("MainActivity", "LaunchedEffect - pendingNotificationId: $pendingNotificationId")
                            pendingNotificationId?.let { id ->
                                val type = pendingNotificationIntent?.getStringExtra("notification_type")
                                Log.d("MainActivity", "처리 시작 - type: $type, id: $id")
                                if (type == "WALKIE") {
                                    val audioId = pendingNotificationIntent?.getStringExtra("notification_audio_id") ?: ""
                                    Log.d("MainActivity", "audioId: $audioId")
                                    if (audioId.isNotEmpty()) {
                                        notificationViewModel.onClickAudioNotification(id, audioId)   }                             } else {
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