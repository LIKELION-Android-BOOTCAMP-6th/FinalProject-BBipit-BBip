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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.bbip.bbipit.util.WatchPermissionUtil

/**
 * 워치 무전기 전송 버튼 컴포넌트
 */
@Composable
fun WatchWalkieTalkieButton(
    viewModel: WatchVoiceViewModelWatch,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var startTime by remember { mutableLongStateOf(0L) }

    // 권한 요청을 위한 전체 권한 리스트
    val requiredPermissions = remember {
        mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()
    }

    // 다중 권한 요청 런처 및 결과 처리
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true

        if (micGranted) {
            startTime = System.currentTimeMillis()
            viewModel.startVoiceTransmission()
        } else {
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
                color = if (uiState.isRecording) MaterialTheme.colors.error
                else MaterialTheme.colors.primary,
                shape = CircleShape
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // 터치 다운 이벤트 감지
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // 오디오 권한 검사 수행
                        val hasMicPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasMicPermission) {
                            startTime = System.currentTimeMillis()

                            // 음성 전송 시작
                            viewModel.startVoiceTransmission()

                            // 터치 해제 시점까지 이벤트 추적
                            var isReleased = false
                            while (!isReleased) {
                                val event = awaitPointerEvent()
                                val anyPressed = event.changes.any { it.pressed }
                                if (!anyPressed) {
                                    isReleased = true
                                }
                            }

                            // 터치 해제 후 전송 종료 처리
                            val endTime = System.currentTimeMillis()
                            val durationMs = endTime - startTime

                            if (durationMs < 500) {
                                Toast.makeText(context, "너무 짧게 누르면 무전이 가지 않습니다.", Toast.LENGTH_SHORT).show()
                                viewModel.stopVoiceTransmission()
                            } else {
                                viewModel.stopVoiceTransmission()
                            }
                        } else {
                            // 권한 요청 팝업 실행
                            permissionLauncher.launch(requiredPermissions)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                uiState.isRecording -> "송신 중..."
                uiState.isUploading -> "전송 중..."
                else -> "무전기\nPUSH"
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.button,
            color = MaterialTheme.colors.onPrimary
        )
    }
}
