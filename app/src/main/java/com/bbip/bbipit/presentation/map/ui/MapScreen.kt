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

    var clickedFriend by remember { mutableStateOf<LiveStatus?>(null) }

    // 비즈니스 전송 에러 발생 시 처리 스크립트
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
            Log.d("MapScreen", "권한 승인됨 -> BackgroundListenerService 가동")
            val intent = Intent(context, BackgroundListenerService::class.java)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(intent)
//            } else {
//                context.startService(intent)
//            }
        } else {
            Toast.makeText(context, "서비스 이용을 위해 위치 및 블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // contentWindowInsets를 0으로 설정하여 시스템 UI 영역까지 콘텐츠가 채워지도록 함
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        BackgroundBox {
            Box(modifier = Modifier.fillMaxSize()) {
                // 구글 지도 및 마커 콘텐츠 레이어
                MapContent(
                    mapUiState = uiState,
                    modifier = Modifier.fillMaxSize(),
                    onFriendClick = { friend ->
                        clickedFriend = friend
                    }
                )
            }

            // 친구 정보 상세 다이얼로그
            clickedFriend?.let { friend ->
                FriendProfileDialog(
                    friend = friend,
                    voiceUiState = voiceUiState,
                    voiceViewModel = pushToTalkViewModel,
                    onDismiss = { clickedFriend = null },
                    onChatClick = {
                        clickedFriend = null
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

        // 위치 권한 최소 하나 이상 승인 상태 기준의 백그라운드 서비스 안전 가동 처리
        if (hasFineLocation || hasCoarseLocation) {
            Log.d("MapScreen", "✅ 위치 권한 확인 완료 -> 안전하게 서비스 시작")
            val intent = Intent(context, BackgroundListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            // 위치 권한 부재 시 통합 권한 요청 팝업 런처 작동 (블루투스, 마이크, 위치 세트)
            Log.d("MapScreen", "⚠️ 위치 권한 없음 -> 권한 요청 팝업 실행")
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

    // 위도 및 경도 좌표 자체의 유효성, 변화 관찰 목적의 정밀 타겟 변수 설정
    val myLat = mapUiState.myStatus?.latitude
    val myLng = mapUiState.myStatus?.longitude

    LaunchedEffect(myLat, myLng) {
        if (myLat != null && myLng != null) {
            Log.d("MapScreen", "🎯 실제 내 위치 포착 완료 -> 카메라 이동: $myLat, $myLng")
            cameraPositionState.animate(
                update = newLatLngZoom(
                    LatLng(myLat, myLng), 16f
                ),
                durationMs = 500 // 부드러운 카메라 스크롤 유저 경험 확보 목적의 시간 설정
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            mapUiState.myStatus?.let { my ->
                // 내 프로필 사진 및 상태 기반 마커 아이콘 기억(remember) 처리
                var myCustomMarkerIcon by remember(my.uid, my.profileImageUrl) {
                    mutableStateOf<BitmapDescriptor?>(null)
                }
                LaunchedEffect(my.profileImageUrl) {
                    myCustomMarkerIcon = createCustomMarkerBitmap(
                        context = context,
                        imageUrl = my.profileImageUrl,
                        isOnline = true // 내 상태 온라인 고정 표기
                    )
                }

                // 위도·경도 좌표 변경 시점 기준 MarkerState 객체 갱신 유도 키(Key) 지정
                val myMarkerState = remember(my.latitude, my.longitude) {
                    MarkerState(position = LatLng(my.latitude, my.longitude))
                }

                Marker(
                    state = myMarkerState,
                    // 이미지 로딩 전 대체용 구글 기본 마커 배치
                    icon = myCustomMarkerIcon ?: BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE
                    ),
                    onClick = { true }
                )
            }

            mapUiState.friendsStatuses.forEach { friend ->

                // 친구 마커 실시간 좌표 변경 시 동기 트래킹 보장 목적의 MarkerState 갱신 키 지정
                val friendMarkerState = remember(friend.latitude, friend.longitude) {
                    MarkerState(position = LatLng(friend.latitude, friend.longitude))
                }
                // 친구 마커용 커스텀 프로필 마커 구조 단일화 목적의 아이콘 변수 생성
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
                        Log.d("MapScreen", "친구 마커 클릭됨: ${friend.nickname} (UID: ${friend.uid})")
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
                    .background(Color.White.copy(alpha = 0.5f)), // 부자연스러운 화면 튐 방지용 반투명 가림막 배치
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

    // 마이크 하드웨어 권한 제어용 런처
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
                    .fillMaxWidth(0.85f) // 화면 가로 영역 대비 85% 강제 지정
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
                                // 카드 크기 변화 대응 목적의 비율제 가로폭 지정
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

                        // 하단 액션 버튼 영역
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