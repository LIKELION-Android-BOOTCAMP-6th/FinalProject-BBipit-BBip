package com.bbip.bbipit.presentation.map.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bbip.bbipit.core.base.MobileAudioReceiverService
import com.bbip.bbipit.core.util.AudioRecorder
import com.bbip.bbipit.core.util.PermissionUtil
import com.bbip.bbipit.presentation.base.BackgroundBox
import com.bbip.bbipit.presentation.map.viewmodel.VoiceViewModel
import com.bbip.bbipit.presentation.test.FeatureTestViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.File

/**
 * 홈 스크린 (지도 및 무전기 기능)
 */
@Composable
fun MapScreen(
    navController: NavController,
    testViewModel: FeatureTestViewModel = hiltViewModel(),
    voiceViewModel: VoiceViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Android 14 이상 대응을 위한 블루투스 및 마이크 통합 권한 요청 런처
    val requestMultiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val bluetoothConnectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false

        // 서비스 제어 필수 블루투스 권한 확인 및 서비스 실행
        if (bluetoothConnectGranted) {
            val intent = Intent(context, MobileAudioReceiverService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            Toast.makeText(context, "워치 무전기 수신을 위해 블루투스 연결 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        BackgroundBox {
            MapContent(modifier = Modifier.padding(innerPadding))

            WalkieTalkieButton(
                viewModel = voiceViewModel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 340.dp)
            )
        }
    }

    LaunchedEffect(Unit) {
        // 로그인 및 타겟 설정
        val isLoginSuccess = testViewModel.testLogin("test_a@example.com", "123456")
        if (isLoginSuccess) {
            val targetUser = testViewModel.testGetUserProfile("Wy102dzyw4buC0V6YJuqxjtf6qA2")
            targetUser?.let { user ->
                voiceViewModel.setTargetUser(user)
            }
        }

        // 기기 버전에 따른 권한 확인 및 서비스 실행 분기 처리
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBluetoothPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            if (hasBluetoothPermission) {
                // 권한 보유 시 서비스 즉시 실행
                val intent = Intent(context, MobileAudioReceiverService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                // 미보유 시 권한 요청 팝업 노출
                requestMultiplePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }
        } else {
            // 하위 버전 서비스 실행
            val intent = Intent(context, MobileAudioReceiverService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

@Composable
private fun MapContent(modifier: Modifier = Modifier) {
    val seoul = LatLng(37.5665, 126.9780)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 15f)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "홈",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge
        )
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        )
    }
}

@Composable
fun WalkieTalkieButton(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val recorder = remember { AudioRecorder(context) }
    val voiceFile = remember { File(context.cacheDir, "temp_recording.m4a") }
    var startTime by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            PermissionUtil.handlePermissionDenial(
                activity = context as Activity,
                permission = Manifest.permission.RECORD_AUDIO,
                onShowToast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(120.dp)
            .shadow(8.dp, CircleShape)
            .background(
                color = when {
                    uiState.isRecording -> MaterialTheme.colorScheme.error
                    uiState.isUploading -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                },
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val permission = Manifest.permission.RECORD_AUDIO
                        val isPermissionGranted = ContextCompat.checkSelfPermission(
                            context,
                            permission
                        ) == PackageManager.PERMISSION_GRANTED

                        if (isPermissionGranted) {
                            if (uiState.selectedTarget == null) {
                                Toast.makeText(context, "전송할 대상이 없습니다.", Toast.LENGTH_SHORT).show()
                                return@detectTapGestures
                            }

                            startTime = System.currentTimeMillis()
                            recorder.start(voiceFile)
                            viewModel.startRecording()
                            try {
                                awaitRelease()
                            } finally {
                                recorder.stop()
                                val endTime = System.currentTimeMillis()
                                val duration =
                                    ((endTime - startTime) / 1000).toInt().coerceAtLeast(1)

                                val uri = if (voiceFile.exists() && voiceFile.length() > 0) {
                                    Uri.fromFile(voiceFile)
                                } else {
                                    null
                                }

                                viewModel.stopRecording(uri, duration)
                            }
                        } else {
                            permissionLauncher.launch(permission)
                        }
                    }
                )
            }
    ) {
        Text(
            text = when {
                uiState.isRecording -> "송신 중..."
                uiState.isUploading -> "전송 중..."
                else -> "무전기\nPUSH"
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
