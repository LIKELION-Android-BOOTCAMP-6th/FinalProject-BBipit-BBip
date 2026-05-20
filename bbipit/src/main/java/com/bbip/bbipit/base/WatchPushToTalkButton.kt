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
 * 실시간 무전 송신 및 터치 이벤트를 처리하는 워치용 푸시투토크(PTT) 버튼 컴포저블
 */
@Composable
fun WatchPushToTalkButton(
    viewModel: WatchVoiceViewModelWatch,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 무전 제약 시간 계산을 위한 터치 시작 시점 기록 변수
    var startTime by remember { mutableLongStateOf(0L) }

    // 하드웨어 제어 및 오디오 녹음을 위한 SDK 버전별 필수 권한 배열 선언
    val requiredPermissions = remember {
        mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()
    }

    // 권한 요청 결과에 따른 무전 실행 및 예외 처리 가이드 루틴 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true

        if (micGranted) {
            startTime = System.currentTimeMillis()
            viewModel.startVoiceTransmission()
        } else {
            // 마이크 권한 거부 시 시스템 안내 토스트 출력 처리
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
                // 상태 변동(송신/전송/대기)에 따른 실시간 UI 배경 색상 분기 매핑
                color = when {
                    uiState.isRecording -> Color(0xFFFF5252) // 음성 녹음 진행 상태: 빨간색
                    uiState.isUploading -> Color(0xFFFFA000) // 파일 업로드 진행 상태: 주황색
                    else -> Color(0xFF9162FF)                // 사용자 입력 대기 상태: 보라색
                },
                shape = CircleShape
            )
            .pointerInput(Unit) {
                // 저수준 포인터 입력을 통한 하향(Down)/상향(Up) 제스처 추적 스코프
                awaitPointerEventScope {
                    while (true) {
                        // 사용자 터치 다운 이벤트 최초 감지
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // 무전 기능 수행을 위한 마이크 권한 보유 상태 검사
                        val hasMicPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasMicPermission) {
                            startTime = System.currentTimeMillis()

                            // 음성 녹음 및 스트리밍 파이프라인 개시 요청
                            viewModel.startVoiceTransmission()

                            // 포인터 눌림 해제 시점까지 실시간 상태 모니터링 반복 루프
                            var isReleased = false
                            while (!isReleased) {
                                val event = awaitPointerEvent()
                                val anyPressed = event.changes.any { it.pressed }
                                if (!anyPressed) {
                                    isReleased = true
                                }
                            }

                            // 터치 해제 감지 후 무전 송신 종료 처리 및 소요 시간 연산
                            val endTime = System.currentTimeMillis()
                            val durationMs = endTime - startTime

                            // 오작동 및 하드웨어 버퍼 보호를 위한 단시간 입력 차단 가드레일
                            if (durationMs < 500) {
                                Toast.makeText(context, "너무 짧게 누르면 무전이 가지 않습니다.", Toast.LENGTH_SHORT).show()
                                viewModel.stopVoiceTransmission()
                            } else {
                                viewModel.stopVoiceTransmission()
                            }
                        } else {
                            // 권한 미보유 상태일 경우 런타임 권한 요청 컴포넌트 호출
                            permissionLauncher.launch(requiredPermissions)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 현재 무전 프로세스 상태에 따른 안내 텍스트 출력 분기 설정
        Text(
            text = when {
                uiState.isRecording -> "송신 중..."
                uiState.isUploading -> "전송 중..."
                else -> "무전"
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.button,
            color = Color.White // 가독성 확보를 위한 텍스트 단색 고정
        )
    }
}