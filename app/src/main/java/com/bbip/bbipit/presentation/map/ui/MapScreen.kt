package com.bbip.bbipit.presentation.map.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.bbip.bbipit.core.base.BackgroundListenerService
import com.bbip.bbipit.core.base.createCustomMarkerBitmap
import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.presentation.base.BackgroundBox
import com.bbip.bbipit.presentation.map.viewmodel.MapUiState
import com.bbip.bbipit.presentation.map.viewmodel.MapViewModel
import com.bbip.bbipit.presentation.map.viewmodel.VoiceUiState
import com.bbip.bbipit.presentation.map.viewmodel.PushToTalkViewModel
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel(),
    pushToTalkViewModel: PushToTalkViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val voiceUiState by pushToTalkViewModel.uiState.collectAsState()

    var clickedFriendUid by remember { mutableStateOf<String?>(null) }

    val TAG = "MapScreen"

    // 음성 전송 실패 에러 발생 시 알림 처리
    LaunchedEffect(voiceUiState.error) {
        voiceUiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            pushToTalkViewModel.clearError()
        }
    }

    val requestMultiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val bluetoothConnectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false

        if (fineLocationGranted || coarseLocationGranted || bluetoothConnectGranted) {
            Log.d(TAG, "권한 승인됨 -> BackgroundListenerService 가동")
            val intent = Intent(context, BackgroundListenerService::class.java)
        } else {
            Toast.makeText(context, "서비스 이용을 위해 위치 및 블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // 시스템 UI 영역까지 콘텐츠를 채우도록 인셋을 0으로 설정
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        BackgroundBox {
            Box(modifier = Modifier.fillMaxSize()) {
                // 지도 및 마커 레이어 노출
                MapContent(
                    mapUiState = uiState,
                    modifier = Modifier.fillMaxSize(),
                    onFriendClick = { friend ->
                        clickedFriendUid = friend.uid
                    }
                )
            }

            // 선택된 친구의 최신 정보 조회
            val currentClickedFriend = remember(clickedFriendUid, uiState.friendsStatuses) {
                uiState.friendsStatuses.find { it.uid == clickedFriendUid }
            }

            // 친구 상세 프로필 다이얼로그 표시
            currentClickedFriend?.let { friend ->
                FriendProfileDialog(
                    friend = friend,
                    voiceUiState = voiceUiState,
                    voiceViewModel = pushToTalkViewModel,
                    onDismiss = { clickedFriendUid = null },
                    onChatClick = {
                        clickedFriendUid = null
                        Toast.makeText(context, "${friend.uid} 채팅 방으로 이동..", Toast.LENGTH_SHORT)
                            .show()
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // 위치 권한 승인 상태에 따른 백그라운드 서비스 실행 코드
        if (hasFineLocation || hasCoarseLocation) {
            Log.d(TAG, "✅ 위치 권한 확인 완료 -> 안전하게 서비스 시작")
            val intent = Intent(context, BackgroundListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            // 위치 권한이 없는 경우 통합 권한 요청 팝업 실행
            Log.d(TAG, "⚠️ 위치 권한 없음 -> 권한 요청 팝업 실행")
            requestMultiplePermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }
}

@Composable
private fun MapContent(
    mapUiState: MapUiState,
    modifier: Modifier = Modifier,
    onFriendClick: (LiveStatus) -> Unit
) {
    val seoul = LatLng(37.5665, 126.9780)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 15f)
    }

    val context = LocalContext.current

    // 내 위치 좌표 추출
    val myLat = mapUiState.myStatus?.latitude
    val myLng = mapUiState.myStatus?.longitude

    val TAG = "MapContent"

    LaunchedEffect(myLat, myLng) {
        if (myLat != null && myLng != null) {
            Log.d(TAG, "🎯 실제 내 위치 포착 완료 -> 카메라 이동: $myLat, $myLng")
            // 현재 내 위치로 지도 카메라 이동
            cameraPositionState.animate(
                update = newLatLngZoom(
                    LatLng(myLat, myLng), 16f
                ),
                durationMs = 500
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            mapUiState.myStatus?.let { my ->
                // 내 프로필 이미지 기반 커스텀 마커 생성
                var myCustomMarkerIcon by remember(my.uid, my.profileImageUrl) {
                    mutableStateOf<BitmapDescriptor?>(null)
                }
                LaunchedEffect(my.profileImageUrl) {
                    myCustomMarkerIcon = createCustomMarkerBitmap(
                        context = context,
                        imageUrl = my.profileImageUrl,
                        isOnline = true
                    )
                }

                // 내 마커 상태 기억 및 설정
                val myMarkerState = remember(my.latitude, my.longitude) {
                    MarkerState(position = LatLng(my.latitude, my.longitude))
                }

                Marker(
                    state = myMarkerState,
                    icon = myCustomMarkerIcon ?: BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE
                    ),
                    onClick = { true }
                )
            }

            mapUiState.friendsStatuses.forEach { friend ->

                // 깜빡임 방지를 위해 UID 기준으로 한 번만 마커 상태 생성 및 유지
                val friendMarkerState = remember(friend.uid) {
                    MarkerState(position = LatLng(friend.latitude, friend.longitude))
                }

                // 실시간 좌표 변경 시 마커 위치 업데이트
                LaunchedEffect(friend.latitude, friend.longitude) {
                    friendMarkerState.position = LatLng(friend.latitude, friend.longitude)
                }

                // 이미지 URL 또는 온라인 여부 변경 시 마커 비트맵 재생성
                var friendCustomMarkerIcon by remember(
                    friend.uid,
                    friend.profileImageUrl,
                    friend.isOnline
                ) {
                    mutableStateOf<BitmapDescriptor?>(null)
                }

                LaunchedEffect(friend.profileImageUrl, friend.isOnline) {
                    friendCustomMarkerIcon = createCustomMarkerBitmap(
                        context = context,
                        imageUrl = friend.profileImageUrl,
                        isOnline = friend.isOnline
                    )
                }

                Marker(
                    state = friendMarkerState,
                    icon = friendCustomMarkerIcon ?: BitmapDescriptorFactory.defaultMarker(
                        if (friend.isOnline) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                    ),
                    onClick = {
                        Log.d(TAG, "친구 마커 클릭됨: ${friend.nickname} (UID: ${friend.uid})")
                        onFriendClick(friend)
                        true
                    }
                )
            }
        }

        if (mapUiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.5f)), // 부자연스러운 화면 깜빡임 방지용 반투명 배경 설정
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun FriendProfileDialog(
    friend: LiveStatus,
    voiceUiState: VoiceUiState,
    voiceViewModel: PushToTalkViewModel,
    onDismiss: () -> Unit,
    onChatClick: () -> Unit
) {
    val context = LocalContext.current
    var startTime by remember { mutableStateOf(0L) }
    val recordAudioPermission = Manifest.permission.RECORD_AUDIO

    // 마이크 권한 요청 런처
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "무전 기능을 이용하려면 마이크 권한 동의가 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // 기본 플랫폼 너비 제한 해제
    ) {
        val windowProvider = LocalView.current.parent as? DialogWindowProvider
        windowProvider?.window?.let { window ->
            window.setDimAmount(0.1f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f) // 화면 가로 영역의 85% 크기 지정
                    .wrapContentHeight()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {},
                shape = RoundedCornerShape(46.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp, vertical = 34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color(0xFF6C727F).copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .clickable { onDismiss() }
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(modifier = Modifier.wrapContentSize()) {
                            val painter = if (friend.profileImageUrl.isNotEmpty()) {
                                rememberAsyncImagePainter(model = friend.profileImageUrl)
                            } else {
                                rememberVectorPainter(image = Icons.Default.Person)
                            }

                            Box(
                                modifier = Modifier
                                    .size(106.dp)
                                    .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                                    .background(Color.White, CircleShape)
                                    .padding(4.5.dp)
                            ) {
                                Image(
                                    painter = painter,
                                    contentDescription = friend.nickname,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = 4.dp, y = 8.dp)
                                    .size(22.dp)
                                    .background(Color.White, CircleShape)
                                    .padding(3.5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = if (friend.isOnline) Color(0xFF00E676) else Color(
                                                0xFF9E9E9E
                                            ),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = friend.nickname,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E232C),
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(
                                    color = Color(0xFFE2E4EE).copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = friend.status.ifEmpty { "등록된 한줄 메세지가 없습니다" },
                                fontSize = 13.sp,
                                color = if (friend.status.isNotEmpty()) Color(0xFF5A6175) else Color(
                                    0xFF94A3B8
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Button(
                                onClick = onChatClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1E232C)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                            ) {
                                Text(
                                    text = "채팅",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1E232C)
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .shadow(2.dp, RoundedCornerShape(28.dp))
                                    .background(
                                        color = when {
                                            voiceUiState.isRecording -> Color(0xFFFF5252)
                                            voiceUiState.isUploading -> Color(0xFFFFA000)
                                            else -> Color(0xFF9162FF)
                                        },
                                        shape = RoundedCornerShape(28.dp)
                                    )
                                    .pointerInput(Unit) {
                                        // 터치 및 홀드 감지를 통한 무전 녹음 제어
                                        detectTapGestures(
                                            onPress = {
                                                val isGranted = ContextCompat.checkSelfPermission(
                                                    context, recordAudioPermission
                                                ) == PackageManager.PERMISSION_GRANTED

                                                if (isGranted) {
                                                    try {
                                                        voiceViewModel.setTargetUid(friend.uid)
                                                        startTime = System.currentTimeMillis()
                                                        voiceViewModel.startRecording()
                                                        awaitRelease()
                                                    } finally {
                                                        val endTime = System.currentTimeMillis()
                                                        val totalDuration =
                                                            ((endTime - startTime) / 1000).toInt()
                                                                .coerceAtLeast(1)
                                                        voiceViewModel.stopRecording(totalDuration)
                                                    }
                                                } else {
                                                    audioPermissionLauncher.launch(
                                                        recordAudioPermission
                                                    )
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Text(
                                    text = when {
                                        voiceUiState.isRecording -> "송신 중..."
                                        voiceUiState.isUploading -> "전송 중..."
                                        else -> "무전"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}