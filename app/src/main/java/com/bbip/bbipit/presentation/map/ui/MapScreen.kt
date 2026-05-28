package com.bbip.bbipit.presentation.map.ui

import com.bbip.bbipit.R
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.bbip.bbipit.core.base.BackgroundListenerService
import com.bbip.bbipit.core.base.createCustomMarkerBitmap
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.presentation.base.BackgroundBox
import com.bbip.bbipit.presentation.main.BottomBarViewModel
import com.bbip.bbipit.presentation.map.viewmodel.HistoryViewModel
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
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberComposeBitmapDescriptor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    mapViewModel: MapViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
    pushToTalkViewModel: PushToTalkViewModel = hiltViewModel(),
    bottomBarViewModel: BottomBarViewModel =
        hiltViewModel(viewModelStoreOwner = (LocalActivity.current as ComponentActivity)),
) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsState()
    val voiceUiState by pushToTalkViewModel.uiState.collectAsState()
    val historyUiState by historyViewModel.uiState.collectAsState()

    var clickedFriendUid by remember { mutableStateOf<String?>(null) }

    // 드로어 열림/닫힘 상태 제어용
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var isHistorySheetOpen by remember { mutableStateOf(false) }
    // 히스토리 상세 다이얼로그 제어용 상태
    var isDetailDialogOpen by remember { mutableStateOf(false) }
    var selectedHistory by remember { mutableStateOf<History?>(null) }

    val seoul = LatLng(37.5665, 126.9780)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 15f)
    }

    // 마지막 쿼리 수행 위치
    var lastFetchedLocation by remember { mutableStateOf<LatLng?>(null) }

    val TAG = "MapScreen"

    // 이동 거리 감지 및 데이터 자동 동기화
    val myStatus = uiState.myStatus

    // 두 좌표 간 거리 계산 (Haversine 공식)
    fun calculateDistanceInMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        Log.d("MapScreen", "🗺️ 지도 화면 복귀")

        mapViewModel.fetchLiveStatusAndRefreshCache()
    }

    LaunchedEffect(drawerState.isOpen) {
        bottomBarViewModel.onUpdateDrawerShown(drawerState.isOpen)
    }

    LaunchedEffect(myStatus?.latitude, myStatus?.longitude) {
        if (myStatus != null) {
            val currentLat = myStatus.latitude
            val currentLng = myStatus.longitude

            val lastLoc = lastFetchedLocation
            if (lastLoc == null) {
                // 최초 데이터 요청
                Log.d(TAG, "🚀 최초 위치 포착으로 인한 히스토리 로드 시작")
                historyViewModel.fetchNearbyHistory(currentLat, currentLng)
                lastFetchedLocation = LatLng(currentLat, currentLng)
            } else {
                val distance = calculateDistanceInMeters(
                    lastLoc.latitude, lastLoc.longitude,
                    currentLat, currentLng
                )
                Log.d(TAG, "🏃 현재 이동 거리 체크: ${distance}m")

                // 500m 이동 시 재요청
                if (distance >= 500f) {
                    Log.d(TAG, "🎯 500m 이상 이동 감지! 히스토리 자동 동기화 쿼리 실행")
                    historyViewModel.fetchNearbyHistory(currentLat, currentLng)
                    lastFetchedLocation = LatLng(currentLat, currentLng)
                }
            }
        }
    }

    // 오류 메시지 팝업 출력
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
            Intent(context, BackgroundListenerService::class.java)
        } else {
            Toast.makeText(context, "서비스 이용을 위해 위치 및 블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        BackgroundBox {
            Box(modifier = Modifier.fillMaxSize()) {
                // 지도 콘텐츠 레이어
                MapContent(
                    mapUiState = uiState,
                    histories = historyUiState.nearbyHistories,
                    cameraPositionState = cameraPositionState,
                    modifier = Modifier.fillMaxSize(),
                    onFriendClick = { friend ->
                        clickedFriendUid = friend.uid
                    },
                    onHistoryClick = { history ->
                        selectedHistory = history
                        isDetailDialogOpen = true
                    }
                )

                // 실시간 위치 공유 토글 버튼
                LocationSharingToggleButton(
                    isSharingEnabled = uiState.isLocationSharing,
                    onToggleClick = { isEnabled ->
                        mapViewModel.toggleLocationSharing(isEnabled)
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                )

                // 히스토리 상세 다이얼로그
                if (isDetailDialogOpen && selectedHistory != null) {
                    HistoryDetailDialog(
                        history = selectedHistory!!,
                        currentUserId = uiState.myStatus?.uid.orEmpty(),
                        onDismiss = { isDetailDialogOpen = false },
                        onDelete = { history ->
                            historyViewModel.deleteHistory(history.id)
                            isDetailDialogOpen = false
                        }
                    )
                }

                // 친구 목록 토글 버튼
                FriendListToggleButton(
                    onClick = {
                        scope.launch { drawerState.open() }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .statusBarsPadding()
                        .padding(end = 16.dp, bottom = 220.dp)
                )

                // 작성 페이지 전환 버튼
                FilledIconButton(
                    onClick = {
                        isHistorySheetOpen = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .statusBarsPadding()
                        .padding(end = 16.dp, bottom = 280.dp)
                        .size(50.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(14.dp),
                            clip = false
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_footprints_icon),
                        contentDescription = "히스토리 바텀 시트 열기",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF956AFC)
                    )
                }

                // 위치 업데이트 버튼
                FilledIconButton(
                    onClick = {
                        mapViewModel.refreshCurrentLocationAndSync()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .statusBarsPadding()
                        .padding(end = 16.dp, bottom = 160.dp)
                        .size(50.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(14.dp),
                            clip = false
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "위치 업데이트",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF956AFC)
                    )
                }
            }

            if (drawerState.isOpen) {
                Popup(
                    alignment = Alignment.TopStart,
                    onDismissRequest = {
                        scope.launch { drawerState.close() }
                    },
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        clippingEnabled = false
                    )
                ) {
                    // 투명한 전체 화면 배경을 만들어 상태바와 네비게이션 바를 덮습니다.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                scope.launch { drawerState.close() }
                            }
                    ) {
                        FriendListDrawer(
                            friends = uiState.friendsStatuses,
                            selectedFriendUid = clickedFriendUid,
                            onCloseClick = {
                                scope.launch { drawerState.close() }
                            },
                            onFriendClick = { friend ->
                                scope.launch {
                                    clickedFriendUid = friend.uid
                                    drawerState.close()
                                    cameraPositionState.animate(
                                        update = newLatLngZoom(
                                            LatLng(
                                                friend.latitude,
                                                friend.longitude
                                            ), 16f
                                        )
                                    )
                                }
                            },
                            // ★ 중요: FriendListDrawer 내부에 전체 화면 높이를 강제 전달하기 위해 modifier 확장
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(280.dp)
                        )
                    }
                }
            }

            // 입력 폼 바텀 시트
            HistoryWriteSheet(
                isOpen = isHistorySheetOpen,
                onDismissRequest = { isHistorySheetOpen = false },
                onSaveClick = { selectedCategory, placeName, contentText ->
                    if (contentText.trim().isEmpty()) {
                        Toast.makeText(context, "히스토리 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                        return@HistoryWriteSheet
                    }

                    uiState.myStatus?.let { myStatus ->
                        historyViewModel.createNewHistory(
                            category = selectedCategory,
                            placeName = placeName.ifEmpty { "알 수 없음" },
                            content = contentText,
                            latitude = myStatus.latitude,
                            longitude = myStatus.longitude
                        )
                        isHistorySheetOpen = false
                    }
                }
            )

            val currentClickedFriend = remember(clickedFriendUid, uiState.friendsStatuses) {
                uiState.friendsStatuses.find { it.uid == clickedFriendUid }
            }

            // 상세 정보 다이얼로그
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

    // 시스템 권한 확인 및 백그라운드 서비스 제어
    LaunchedEffect(Unit) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // 알림 권한 체크 추가 (API 33 이상 대응)
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasFineLocation && hasNotificationPermission) {
            Log.d(TAG, "✅ 필요한 모든 권한 확인 완료 -> 안전하게 서비스 시작")
            val intent = Intent(context, BackgroundListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            Log.d(TAG, "⚠️ 권한 부족 -> 권한 요청 팝업 실행")

            val permissionsToRequest = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                // 안드로이드 13 이상일 때만 알림 권한 추가
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()

            requestMultiplePermissionsLauncher.launch(permissionsToRequest)
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun MapContent(
    mapUiState: MapUiState,
    histories: List<History>,
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    onFriendClick: (LiveStatus) -> Unit,
    onHistoryClick: (History) -> Unit
) {
    val context = LocalContext.current

    val myLat = mapUiState.myStatus?.latitude
    val myLng = mapUiState.myStatus?.longitude

    var isCameraInitialized by remember { mutableStateOf(false) }

    val TAG = "MapContent"

    // 내 위치 포착 시 카메라 이동
    LaunchedEffect(myLat, myLng) {
        if (myLat != null && myLng != null) {
            if (!isCameraInitialized) {
                Log.d(TAG, "🎯 최초 위치 즉시 조준 (Snap) -> $myLat, $myLng")
                cameraPositionState.move(
                    update = newLatLngZoom(LatLng(myLat, myLng), 16f)
                )
                isCameraInitialized = true
            }
            else {
                Log.d(TAG, "🎯 실제 내 위치 포착 완료 -> 카메라 이동: $myLat, $myLng")
                cameraPositionState.animate(
                    update = newLatLngZoom(
                        LatLng(myLat, myLng), 16f),
                    durationMs = 500
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // 내 마커 구성
            mapUiState.myStatus?.let { my ->

                var myCustomMarkerIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
                val myProfileUrl = mapUiState.myStatus.profileImageUrl

                // 프로필 URL이 실제로 바뀔 때만 비트맵을 딱 한 번 생성
                LaunchedEffect(myProfileUrl) {
                    myCustomMarkerIcon = createCustomMarkerBitmap(
                        context = context,
                        imageUrl = myProfileUrl,
                        isOnline = true
                    )
                }

                val myMarkerState = remember(my.uid) {
                    MarkerState(position = LatLng(my.latitude, my.longitude))
                }

                // 좌표 업데이트
                LaunchedEffect(my.latitude, my.longitude) {
                    myMarkerState.position = LatLng(my.latitude, my.longitude)
                }

                if (myCustomMarkerIcon != null) {
                    Marker(
                        state = myMarkerState,
                        icon = myCustomMarkerIcon,
                        zIndex = 0.0f,
                        onClick = { true }
                    )
                }
            }

            // 친구 마커 구성
            mapUiState.friendsStatuses.forEach { friend ->
                if (!friend.isSharing) return@forEach
                val friendMarkerState = remember(friend.uid) {
                    MarkerState(position = LatLng(friend.latitude, friend.longitude))
                }

                LaunchedEffect(friend.latitude, friend.longitude) {
                    friendMarkerState.position = LatLng(friend.latitude, friend.longitude)
                }

                var friendCustomMarkerIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

                LaunchedEffect(friend.profileImageUrl, friend.isOnline) {
                    friendCustomMarkerIcon = createCustomMarkerBitmap(
                        context = context,
                        imageUrl = friend.profileImageUrl,
                        isOnline = friend.isOnline
                    )
                }

                if (friendCustomMarkerIcon != null) {
                    Marker(
                        state = friendMarkerState,
                        icon = friendCustomMarkerIcon,
                        zIndex = 1.0f,
                        onClick = {
                            Log.d(TAG, "친구 마커 클릭됨: ${friend.nickname} (UID: ${friend.uid})")
                            onFriendClick(friend)
                            true
                        }
                    )
                }
            }

            // 히스토리 마커 구성
            histories.forEach { history ->
                val historyLatLng = LatLng(history.latitude, history.longitude)

                val markerState = remember(history.id) {
                    MarkerState(position = historyLatLng)
                }

                LaunchedEffect(history.latitude, history.longitude) {
                    markerState.position = historyLatLng
                }

                val bitmapKey = remember(history.id, history.category) {
                    "${history.id}_${history.category}"
                }

                // 카테고리별 아이콘 속성 정의
                val (iconResId, bgColor, iconColor) = remember(history.category) {
                    when (history.category) {
                        "무전" -> Triple(
                            com.bbip.bbipit.R.drawable.ic_walkie_talkie_icon,
                            Color(0xFFFAF5FF),
                            Color(0xFFA855F7)
                        )

                        "카페" -> Triple(
                            com.bbip.bbipit.R.drawable.ic_cafe_icon,
                            Color(0xFFFFFBEB),
                            Color(0xFFD97706)
                        )

                        "음식" -> Triple(
                            com.bbip.bbipit.R.drawable.ic_restaurant_icon,
                            Color(0xFFFFF1F2),
                            Color(0xFFF43F5E)
                        )

                        "운동" -> Triple(
                            com.bbip.bbipit.R.drawable.ic_exercise_icon,
                            Color(0xFFECFDF5),
                            Color(0xFF10B981)
                        )

                        else -> Triple(
                            com.bbip.bbipit.R.drawable.ic_daily_icon,
                            Color(0xFFEEF2FF),
                            Color(0xFF6366F1)
                        )
                    }
                }

                val composeMarkerBitmap = rememberComposeBitmapDescriptor(
                    bitmapKey,
                    bitmapKey
                ) {
                    HistoryIconCircle(
                        iconResId = iconResId,
                        iconTint = iconColor,
                        backgroundColor = bgColor,
                    )
                }

                Marker(
                    state = markerState,
                    title = "[${history.category}] ${history.placeName}",
                    snippet = "${history.userNickname}: ${history.content}",
                    icon = composeMarkerBitmap,
                    alpha = 0.95f,
                    zIndex = 2.0f,
                    onClick = { _ ->
                        onHistoryClick(history)
                        true
                    }
                )
            }
        }
    }
}

// 친구찾기 토글 버튼
@Composable
fun FriendListToggleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonShape = RoundedCornerShape(14.dp)

    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .size(50.dp)
            .shadow(
                elevation = 6.dp,
                shape = buttonShape,
                clip = false
            ),
        shape = buttonShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color(0xFFF1F5F9),
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = "친구 목록 열기",
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF956AFC)
        )
    }
}

// 상세 정보 다이얼로그
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

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context,
                "무전 기능을 이용하려면 마이크 권한 동의가 필요합니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val windowProvider = LocalView.current.parent as? DialogWindowProvider
        windowProvider?.window?.let { window ->
            window.setDimAmount(0.1f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
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
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = CircleShape,
                                        clip = false
                                    )
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
                                            color = if (friend.isOnline) Color(
                                                0xFF00E676
                                            ) else Color(
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
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 1.dp
                                )
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
                                                val isGranted =
                                                    ContextCompat.checkSelfPermission(
                                                        context, recordAudioPermission
                                                    ) == PackageManager.PERMISSION_GRANTED

                                                if (isGranted) {
                                                    try {
                                                        voiceViewModel.setTargetUid(
                                                            friend.uid
                                                        )
                                                        startTime =
                                                            System.currentTimeMillis()
                                                        voiceViewModel.startRecording()
                                                        awaitRelease()
                                                    } finally {
                                                        val endTime =
                                                            System.currentTimeMillis()
                                                        val totalDuration =
                                                            ((endTime - startTime) / 1000).toInt()
                                                                .coerceAtLeast(1)
                                                        voiceViewModel.stopRecording(
                                                            totalDuration
                                                        )
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

// 히스토리 커스텀 마커 아이콘
@Composable
fun HistoryIconCircle(
    @DrawableRes iconResId: Int,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White
) {
    val markerShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = markerShape,
                clip = false
            )
            .size(38.dp)
            .background(backgroundColor, shape = markerShape)
            .padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(iconTint)
        )
    }
}

// 히스토리 상세 다이얼로그
@Composable
fun HistoryDetailDialog(
    history: History,
    currentUserId: String,
    onDismiss: () -> Unit,
    onDelete: (History) -> Unit
) {
    val isMyHistory = remember(history.userId, currentUserId) {
        history.userId == currentUserId && currentUserId.isNotEmpty()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column {
                    AsyncImage(
                        model = history.imageUrl,
                        contentDescription = "History Photo",
                        modifier = Modifier
                            .height(140.dp)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF956AFC)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = history.placeName,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = history.content,
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isMyHistory) {
                                Button(
                                    onClick = { onDelete(history) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFF1F2),
                                        contentColor = Color(0xFFF43F5E)
                                    ),
                                    elevation = null
                                ) {
                                    Text(
                                        "삭제",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Red
                                    )
                                }
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF956AFC),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    "확인",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }

                            if (!isMyHistory) {
                                Spacer(modifier = Modifier.weight(0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 실시간 위치 공유 상태를 토글하는 알약 모양의 커스텀 버튼
 *
 * @param isSharingEnabled 현재 위치 공유 활성화 여부
 * @param onToggleClick 클릭 시 상태 반전을 전달할 콜백 함수
 * @param modifier 외부 레이아웃 배치를 위한 modifier
 */
@Composable
fun LocationSharingToggleButton(
    isSharingEnabled: Boolean,
    onToggleClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // 상태 변경 시 부드러운 색상 전환 효과 애니메이션
    val indicatorColor by animateColorAsState(
        targetValue = if (isSharingEnabled) Color(0xFF00E676) else Color(0xFF94A3B8),
        label = "IndicatorColor"
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(50),
                clip = false
            )
            .background(Color.White, shape = RoundedCornerShape(50))
            .clickable { onToggleClick(!isSharingEnabled) } // 현재 상태를 반전하여 이벤트 전달
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. 상태 인디케이터 점 (On: 초록색, Off: 회색)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = indicatorColor, shape = CircleShape)
            )

            // 2. 상태 텍스트
            Text(
                text = if (isSharingEnabled) "실시간 위치 공유 중" else "위치 공유 꺼짐",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSharingEnabled) Color(0xFF1E232C) else Color(0xFF6C727F),
                letterSpacing = (-0.3).sp
            )
        }
    }
}