package com.bbip.bbipit.base

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.bbip.bbipit.util.WatchPermissionUtil

/**
 * 워치용 푸시투토크(PTT) 버튼 컴포저블
 */
@Composable
fun WatchPushToTalkButton(
    viewModel: WatchVoiceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 터치 시작 시점 기록
    var startTime by remember { mutableLongStateOf(0L) }

    // SDK 버전별 필수 권한 배열
    val requiredPermissions = remember {
        mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()
    }

    // 권한 요청 결과 처리
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true

        if (micGranted) {
            startTime = System.currentTimeMillis()
            viewModel.startVoiceTransmission()
        } else {
            // 마이크 권한 거부 시 토스트 표시
            WatchPermissionUtil.handlePermissionDenial(
                context = context,
                permission = Manifest.permission.RECORD_AUDIO,
                onShowToast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(4.dp, CircleShape)
            .background(
                // 상태별 배경 색상 설정
                color = when {
                    uiState.isRecording -> Color(0xFFFF5252) // 녹음 중: 빨간색
                    uiState.isUploading -> Color(0xFFFFA000) // 업로드 중: 주황색
                    else -> Color(0xFF9162FF)                // 대기 중: 보라색
                },
                shape = CircleShape
            )
            .pointerInput(Unit) {
                // 포인터 입력을 통한 터치 제스처 추적
                awaitPointerEventScope {
                    while (true) {
                        // 터치 다운 이벤트 감지
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // 마이크 권한 확인
                        val hasMicPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasMicPermission) {
                            startTime = System.currentTimeMillis()

                            // 음성 송신 시작
                            viewModel.startVoiceTransmission()

                            // 터치 해제 시점까지 대기
                            var isReleased = false
                            while (!isReleased) {
                                val event = awaitPointerEvent()
                                val anyPressed = event.changes.any { it.pressed }
                                if (!anyPressed) {
                                    isReleased = true
                                }
                            }

                            // 터치 시간 계산
                            val endTime = System.currentTimeMillis()
                            val durationMs = endTime - startTime

                            // 최소 녹음 시간 검증 및 음성 송신 종료
                            if (durationMs < 500) {
                                Toast.makeText(context, "너무 짧게 누르면 무전이 가지 않습니다.", Toast.LENGTH_SHORT).show()
                                viewModel.stopVoiceTransmission()
                            } else {
                                viewModel.stopVoiceTransmission()
                            }
                        } else {
                            // 권한 요청 실행
                            permissionLauncher.launch(requiredPermissions)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 상태별 안내 텍스트 설정
        Text(
            text = when {
                uiState.isRecording -> "송신 중..."
                uiState.isUploading -> "전송 중..."
                else -> "무전"
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.button,
            color = Color.White
        )
    }
}