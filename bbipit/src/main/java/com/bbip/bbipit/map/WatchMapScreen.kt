package com.bbip.bbipit.map

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bbip.bbipit.base.WatchVoiceViewModelWatch
import com.bbip.bbipit.base.WatchWalkieTalkieButton
import com.google.android.gms.wearable.Wearable
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.tasks.await

/**
 * 워치 지도 화면 컴포넌트
 */
@Composable
fun WatchMapScreen(
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState()
    val context = LocalContext.current

    // 전용 수동 팩토리를 이용한 뷰모델 생성
    val voiceViewModel: WatchVoiceViewModelWatch = viewModel(
        factory = WatchVoiceViewModelWatch.provideFactory(context)
    )

    // 화면 진입 시 스마트폰 권한 요청 전송
    LaunchedEffect(Unit) {
        sendPermissionRequestToPhone(context)
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
        )

        // 구글 맵 위 무전기 버튼 배치
        WatchWalkieTalkieButton(
            viewModel = voiceViewModel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * 스마트폰 대상 권한 요청 메시지 전송
 */
suspend fun sendPermissionRequestToPhone(context: Context) {
    val messageClient = Wearable.getMessageClient(context)
    val nodeClient = Wearable.getNodeClient(context)

    try {
        // 연결된 기기 노드 목록 조회
        val nodes = nodeClient.connectedNodes.await()
        val targetNodeId = nodes.firstOrNull()?.id

        if (targetNodeId != null) {
            // 메시지 전송
            messageClient.sendMessage(
                targetNodeId,
                "/request_phone_permission",
                byteArrayOf()
            ).await()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
