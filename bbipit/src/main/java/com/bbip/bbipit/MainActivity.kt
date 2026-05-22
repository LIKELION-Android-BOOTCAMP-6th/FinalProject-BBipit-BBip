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
import com.bbip.bbipit.theme.BbipitTheme
import com.google.android.gms.wearable.Wearable

/**
 * 애플리케이션 수명주기 및 모바일 연동 상태를 제어하는 진입점 액티비티
 */
class MainActivity : ComponentActivity() {

    // 실시간 전역 음성 데이터 수신 처리를 위한 뷰모델 인스턴스 획득
    private val viewModel: MainViewModel by viewModels()

    // 워치 화면의 실시간 활성화(포그라운드) 여부를 기록하는 상태 플래그
    private var isCurrentlyActive = false

    // 연동된 모바일 기기로부터의 워치 상태 확인 요청에 대응하는 통신 리스너 정의
    private val messageListener = com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener { messageEvent ->
        if (messageEvent.path == "/request_watch_status") {
            Log.d("WatchStatus", "📱 폰으로부터 상태 확인 요청 수신 -> 현재 상태($isCurrentlyActive) 응답 처리")
            // 수신된 즉시 현재 포그라운드 가시성 상태값을 모바일로 회신
            sendWatchStateToPhone(isCurrentlyActive)
        }
    }

    /**
     * 스플래시 화면 초기화 및 Wearable 통신 리스너 등록 수행
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        // Wearable 데이터 레이어 레이아웃 기반의 메시지 수신 리스너 등록
        Wearable.getMessageClient(this).addListener(messageListener)

        setContent {
            // 실시간 수신 오디오 상태 데이터 스트림 관찰
            val voiceData by viewModel.voiceUiState.collectAsState()

            WearApp()

            // 유효한 실시간 음성 수신 패킷 도달 시 오디오 수신 상태 알림 팝업 가시화
            voiceData?.let { data ->
                WatchIncomingVoiceDialog(
                    data,
                    onDismiss = {
                        viewModel.clearVoiceState()
                    }
                )
            }
        }
    }

    /**
     * 액티비티 파괴 시 등록된 Wearable 통신 리스너 안전 해제
     */
    override fun onDestroy() {
        super.onDestroy()

        // 메모리 누수 방지를 위한 수신 리스너 제거
        Wearable.getMessageClient(this).removeListener(messageListener)
    }

    /**
     * 연결된 모든 웨어러블 노드를 탐색하여 워치의 현재 활성 상태값을 모바일로 비동기 송신
     */
    private fun sendWatchStateToPhone(isActive: Boolean) {
        val messageClient = Wearable.getMessageClient(this)
        val path = "/watch_state"
        val payload = isActive.toString().toByteArray()

        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    messageClient.sendMessage(node.id, path, payload)
                        .addOnFailureListener { e -> Log.e("WatchStatus", "상태 전송 실패", e) }
                }
            }
            .addOnFailureListener { e ->
                Log.e("WatchStatus", "노드 가져오기 실패", e)
            }
    }

    /**
     * 화면 포그라운드 진입 시 활성 플래그 갱신 및 모바일 상태 동기화 신호 전송
     */
    override fun onResume() {
        super.onResume()
        isCurrentlyActive = true
        sendWatchStateToPhone(true)
    }

    /**
     * 화면 백그라운드 전환 시 활성 플래그 갱신 및 모바일 상태 동기화 신호 전송
     */
    override fun onPause() {
        super.onPause()
        isCurrentlyActive = false
        sendWatchStateToPhone(false)
    }
}

/**
 * 시간 오버레이 및 메인 지도 화면을 배치하는 최상위 UI 레이아웃 컴포저블
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
            // 소형 원형 디스플레이 상단 기본 시스템 시각 표시 컴포넌트
            TimeText()

            // 실시간 유저 위치 추적 및 무전 송수신 통합 지도 컴포넌트 호출
            WatchMapScreen(
                modifier = Modifier.fillMaxSize()
            )
            WatchNotificationOverlay(
                viewModel = viewModel(),
                onBannerClick = { item ->
                }
            )
        }
    }
}