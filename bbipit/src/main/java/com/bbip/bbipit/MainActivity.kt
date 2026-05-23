package com.bbip.bbipit

import android.os.Bundle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import com.bbip.bbipit.base.WatchIncomingVoiceDialog
import com.bbip.bbipit.map.WatchMapScreen
import com.bbip.bbipit.models.WatchLiveStatus
import com.bbip.bbipit.notification.WatchNotificationOverlay
import com.bbip.bbipit.service.WatchCentralService
import com.bbip.bbipit.theme.BbipitTheme
import com.google.android.gms.wearable.Wearable

/**
 * 애플리케이션 진입점 메인 액티비티
 */
class MainActivity : ComponentActivity() {

    // 음성 데이터 수신 처리 뷰모델
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()

            // 실시간 음성 데이터 관찰
            val voiceData by viewModel.voiceUiState.collectAsState()

            // 음성 데이터 수신 시 수신 다이얼로그 표시
            voiceData?.let { data ->
                WatchIncomingVoiceDialog(data, onDismiss = { viewModel.clearVoiceState() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 포그라운드 활성화 상태 선언
        WatchCentralService.isWatchActiveInForeground = true
    }

    override fun onPause() {
        super.onPause()
        // 포그라운드 비활성화 상태 선언
        WatchCentralService.isWatchActiveInForeground = false
    }
}

/**
 * 최상위 레이아웃 컴포저블
 */
@Composable
fun WearApp() {
    BbipitTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            // 시스템 시간 표시 컴포넌트
            TimeText()

            // 워치 지도 화면
            WatchMapScreen(
                modifier = Modifier.fillMaxSize()
            )

            // 알림 오버레이 화면
            WatchNotificationOverlay(
                viewModel = viewModel(),
                onBannerClick = { item ->
                }
            )
        }
    }
}