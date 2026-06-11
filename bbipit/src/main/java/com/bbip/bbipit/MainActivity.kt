package com.bbip.bbipit

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import com.bbip.bbipit.base.WatchIncomingVoiceDialog
import com.bbip.bbipit.base.WatchServiceRestrictedScreen
import com.bbip.bbipit.map.WatchMapScreen
import com.bbip.bbipit.models.MobileServiceStatus
import com.bbip.bbipit.models.WatchVoiceData
import com.bbip.bbipit.theme.BbipitTheme
import com.bbip.bbipit.util.VoiceEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 애플리케이션 진입점 메인 액티비티
 */
class MainActivity : ComponentActivity() {

    // 음성 데이터 수신 처리 뷰모델
    private val viewModel: WatchMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            val mobileStatus by viewModel.mobileStatus.collectAsState()

            WearApp(
                mobileStatus = mobileStatus,
                onExitClick = { finish() }
            )

            // 실시간 음성 데이터 관찰
            val voiceData by viewModel.voiceUiState.collectAsState()

            // 음성 데이터 수신 시 수신 다이얼로그 표시
            voiceData?.let { data ->
                WatchIncomingVoiceDialog(data, onDismiss = { viewModel.clearVoiceState() })
            }
        }
        viewModel.handlePlayIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // 포그라운드 활성화 상태 선언
        viewModel.isWatchActiveInForeground = true

        viewModel.checkPhoneServiceStatus()

        triggerWatchStateTransport(true)
    }

    override fun onPause() {
        super.onPause()
        // 포그라운드 비활성화 상태 선언
        viewModel.isWatchActiveInForeground = false
        triggerWatchStateTransport(false)

    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handlePlayIntent(intent)
    }
    /**
     * 워치의 포그라운드 유무 상태를 폰으로 즉시 쏘아주는 헬퍼 함수
     */
    private fun triggerWatchStateTransport(isActive: Boolean) {
        lifecycleScope.launch {
            try {
                viewModel.sendWatchStateToPhone(isActive)
            } catch (e: Exception) {
                android.util.Log.e("WatchLifecycle", "폰으로 상태 전송 실패", e)
            }
        }
    }
}

/**
 * 최상위 레이아웃 컴포저블
 */
@Composable
fun WearApp(
    mobileStatus: MobileServiceStatus,
    onExitClick: () -> Unit
) {
    BbipitTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            // 시스템 시간 표시 컴포넌트
            TimeText()

//            // 알림 오버레이 화면
//            WatchNotificationOverlay(
//                viewModel = viewModel(),
//                onBannerClick = { item ->
//                }
//            )

            // 💡 상태(State)에 따른 화면 분기 처리
            when (mobileStatus) {
                MobileServiceStatus.CHECKING -> {
                    // 상태를 확인 중일 때 보여줄 간이 로딩 인디케이터나 기본 대기 화면
                    androidx.wear.compose.material3.CircularProgressIndicator()
                }
                MobileServiceStatus.READY -> {
                    // 정상 상태일 때 기존 워치 지도 화면 실행
                    WatchMapScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MobileServiceStatus.SERVICE_RESTRICTED -> {
                    // 서비스 제한 상태일 때 제한 화면 실행 및 종료 이벤트 연결
                    WatchServiceRestrictedScreen(
                        onExitClick = onExitClick
                    )
                }
            }
        }
    }
}