package com.bbip.bbipit.presentation.map.ui

import com.bbip.bbipit.R
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.bbip.bbipit.core.base.BackgroundListenerService
import com.bbip.bbipit.core.base.createCustomMarkerBitmap
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.fontDefault
import com.bbip.bbipit.core.ui.theme.online
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.core.ui.theme.recording
import com.bbip.bbipit.core.ui.theme.send
import com.bbip.bbipit.core.ui.theme.subBackground
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.presentation.base.BackgroundBox
import com.bbip.bbipit.presentation.base.ConfirmDialog
import com.bbip.bbipit.presentation.base.ShowToast
import com.bbip.bbipit.presentation.main.BottomBarViewModel
import com.bbip.bbipit.presentation.map.viewmodel.HistoryViewModel
import com.bbip.bbipit.presentation.map.viewmodel.MapUiState
import com.bbip.bbipit.presentation.map.viewmodel.MapViewModel
import com.bbip.bbipit.presentation.map.viewmodel.VoiceUiState
import com.bbip.bbipit.presentation.map.viewmodel.PushToTalkViewModel
import com.bbip.bbipit.presentation.permission.PermissionRequestScreen
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberComposeBitmapDescriptor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
    pushToTalkViewModel: PushToTalkViewModel = hiltViewModel(),
    bottomBarViewModel: BottomBarViewModel =
        hiltViewModel(viewModelStoreOwner = (LocalActivity.current as ComponentActivity)),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val voiceUiState by pushToTalkViewModel.uiState.collectAsState()
    val historyUiState by historyViewModel.uiState.collectAsState()

    var clickedFriendUid by remember { mutableStateOf<String?>(null) }
//    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isDrawerRendering by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var isHistorySheetOpen by remember { mutableStateOf(false) }
    var isDetailDialogOpen by remember { mutableStateOf(false) }
    var selectedHistory by remember { mutableStateOf<History?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val seoul = LatLng(37.5665, 126.9780)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 15f)
    }

    var lastFetchedLocation by remember { mutableStateOf<LatLng?>(null) }
    val TAG = "MapScreen"
    val myStatus = uiState.myStatus

    fun calculateDistanceInMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    val checkAndStartService = {
        val hasLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocation && hasNotificationPermission && hasAudioPermission) {
            Log.d(TAG, "✅ 권한 확인 완료 -> 백그라운드 서비스 시작")
            showPermissionDialog = false
            val intent = Intent(context, BackgroundListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            showPermissionDialog = true
        }
    }

    val requestMultiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if ((fineLocationGranted || coarseLocationGranted) && notificationGranted && audioGranted) {
            checkAndStartService()
        } else {
            showPermissionDialog = false
            navController.navigate(Routes.ServiceRestricted)
        }
    }

    LaunchedEffect(Unit) {
        checkAndStartService()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.fetchLiveStatusAndRefreshCache()
    }

//    LaunchedEffect(drawerState.isOpen) {
//        bottomBarViewModel.onUpdateDrawerShown(drawerState.isOpen)
//    }
    LaunchedEffect(isDrawerOpen) {
        bottomBarViewModel.onUpdateDrawerShown(isDrawerOpen)
    }
    // 드로어 상태가 바뀔 때 렌더링 플래그를 동기화합니다.
    LaunchedEffect(isDrawerOpen) {
        if (isDrawerOpen) {
            isDrawerRendering = true
        }
    }

    LaunchedEffect(myStatus?.latitude, myStatus?.longitude) {
        if (myStatus != null) {
            val currentLat = myStatus.latitude
            val currentLng = myStatus.longitude
            val lastLoc = lastFetchedLocation

            if (lastLoc == null) {
                historyViewModel.fetchNearbyHistory(currentLat, currentLng)
                lastFetchedLocation = LatLng(currentLat, currentLng)
            } else {
                val distance = calculateDistanceInMeters(
                    lastLoc.latitude,
                    lastLoc.longitude,
                    currentLat,
                    currentLng
                )
                if (distance >= 500f) {
                    historyViewModel.fetchNearbyHistory(currentLat, currentLng)
                    lastFetchedLocation = LatLng(currentLat, currentLng)
                }
            }
        }
    }

    LaunchedEffect(voiceUiState.error) {
        voiceUiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            pushToTalkViewModel.clearError()
        }
    }

    if (uiState.isStopSharingDialogShown && uiState.isLocationSharing) {
        ConfirmDialog(
            text = "위치 공유를 중지하시겠습니까?",
            semiText = "위치 공유를 중지할 경우 \n친구의 위치를 알 수 없습니다.",
            onDismiss = { viewModel.onUpdateStopSharingDialog(false) },
            onConfirm = {
                viewModel.onUpdateStopSharingDialog(false)
                viewModel.toggleLocationSharing(false)
            }
        )
    }

    if (showPermissionDialog) {
        PermissionRequestScreen(
            onGrantPermission = {
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.RECORD_AUDIO
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
                requestMultiplePermissionsLauncher.launch(permissionsToRequest)
            },
            onDismiss = {
                showPermissionDialog = false
                navController.navigate(Routes.ServiceRestricted)
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        BackgroundBox {
            Box(modifier = Modifier.fillMaxSize()) {
                MapContent(
                    mapUiState = uiState,
                    histories = historyUiState.nearbyHistories,
                    cameraPositionState = cameraPositionState,
                    modifier = Modifier.fillMaxSize(),
                    onFriendClick = { friend -> clickedFriendUid = friend.uid },
                    onHistoryClick = { history ->
                        selectedHistory = history
                        isDetailDialogOpen = true
                    }
                )

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

                LocationSharingToggleButton(
                    isSharingEnabled = uiState.isLocationSharing,
                    onToggleClick = { isEnable ->
                        if (uiState.isLocationSharing) {
                            viewModel.onUpdateStopSharingDialog(true)
                        } else {
                            viewModel.toggleLocationSharing(isEnable)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                )

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

                FriendListToggleButton(
                    onClick = {
                        scope.launch {
                            isDrawerOpen = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .statusBarsPadding()
                        .padding(end = 16.dp, bottom = 220.dp)
                )

                FilledIconButton(
                    onClick = {
                        viewModel.refreshCurrentLocationAndSync()
                        uiState.myStatus?.let { my ->
                            scope.launch {
                                cameraPositionState.animate(
                                    update = newLatLngZoom(
                                        LatLng(my.latitude, my.longitude),
                                        16f
                                    ),
                                    durationMs = 500
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .statusBarsPadding()
                        .padding(end = 16.dp, bottom = 160.dp)
                        .size(50.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(14.dp), clip = false),
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "위치 업데이트",
                        modifier = Modifier.size(24.dp),
                        tint = primary
                    )
                }
            }

            if (isDrawerOpen || isDrawerRendering) {
                Popup(
                    alignment = Alignment.TopStart,
                    onDismissRequest = { isDrawerOpen = false },
                    properties = PopupProperties(
                        focusable = isDrawerOpen,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        clippingEnabled = false,
                        usePlatformDefaultWidth = false
                    )
                ) {
                    var startAnimate by remember { mutableStateOf(false) }
                    LaunchedEffect(isDrawerOpen) {
                        if (isDrawerOpen) {
                            // 가드레일 적용: Popup Window가 안드로이드 서페이스에 안착할 시간을 계산 (대략 1~2프레임)
                            kotlinx.coroutines.delay(30)
                            startAnimate = true
                        } else {
                            startAnimate = false
                        }
                    }

                    // 2. 뒷배경 딤 애니메이션
                    val scrimColor by animateColorAsState(
                        targetValue = if (startAnimate) Color.Black.copy(alpha = 0.4f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 300),
                        label = "ScrimColor",
                        finishedListener = {
                            // 닫히는 애니메이션이 완전히 끝나면 팝업을 트리에서 제거
                            if (!isDrawerOpen) {
                                isDrawerRendering = false
                            }
                        }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scrimColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isDrawerOpen = false
                            }
                    ) {
                        val closeDurationMillis = if (clickedFriendUid != null) 0 else 300
                        // 왼쪽에서 오른쪽으로 슬라이드 인/아웃 되는 애니메이션 효과 추가
                        AnimatedVisibility(
                            visible = startAnimate,
                            enter = slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(durationMillis = 300)
                            ),
                            exit = slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(durationMillis = closeDurationMillis)
                            )
                        ) {
                            FriendListDrawer(
                                friends = uiState.friendsStatuses,
                                selectedFriendUid = clickedFriendUid,
                                onCloseClick = { isDrawerOpen = false },
                                onFriendClick = { friend ->
                                    scope.launch {
                                        clickedFriendUid = friend.uid
                                        isDrawerOpen = false
                                        cameraPositionState.animate(
                                            update = newLatLngZoom(
                                                LatLng(friend.latitude, friend.longitude),
                                                16f
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(280.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {}
                            )
                        }
                    }
                }
            }

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
                uiState.friendsStatuses.find { it.uid == clickedFriendUid && it.isSharing }
            }

            // 선택된 친구가 위치공유를 중단한 경우 clickedFriendUid null 처리
            LaunchedEffect(clickedFriendUid, currentClickedFriend) {
                if (clickedFriendUid != null && currentClickedFriend == null) {
                    clickedFriendUid = null
                }
            }

            currentClickedFriend?.let { friend ->
                FriendProfileDialog(
                    friend = friend,
                    voiceUiState = voiceUiState,
                    voiceViewModel = pushToTalkViewModel,
                    onDismiss = { clickedFriendUid = null },
                    onChatClick = {
                        viewModel.createOrGetChatRoom(
                            targetUid = friend.uid,
                            onSuccess = { roomId ->
                                clickedFriendUid = null
                                navController.navigate(
                                    Routes.ChatRoom(
                                        roomId = roomId,
                                        receiverId = friend.uid
                                    )
                                )
                            },
                            onError = { errorMessage ->
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun MapContent(
    mapUiState: MapUiState,
    histories: List<History>,
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    onFriendClick: (LiveStatus) -> Unit,
    onHistoryClick: (History) -> Unit
) {
    val myLat = mapUiState.myStatus?.latitude
    val myLng = mapUiState.myStatus?.longitude
    var isCameraInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(myLat, myLng) {
        if (myLat != null && myLng != null) {
            if (!isCameraInitialized) {
                cameraPositionState.move(update = newLatLngZoom(LatLng(myLat, myLng), 16f))
                isCameraInitialized = true
            } else {
                cameraPositionState.animate(
                    update = newLatLngZoom(LatLng(myLat, myLng), 16f),
                    durationMs = 500
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false
            )
        ) {
            mapUiState.myStatus?.let { my ->
                MyMarker(myStatus = my, profileImageUrl = my.profileImageUrl)
            }

            mapUiState.friendsStatuses.forEach { friend ->
                if (friend.isSharing) {
                    key(friend.uid) {
                        FriendMarker(friend = friend, onFriendClick = onFriendClick)
                    }
                }
            }

            histories.forEach { history ->
                key(history.id) {
                    HistoryMarker(history = history, onHistoryClick = onHistoryClick)
                }
            }
        }
    }
}

@Composable
private fun MyMarker(myStatus: LiveStatus, profileImageUrl: String) {
    val context = LocalContext.current
    var myCustomMarkerIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

//    val myMarkerState = remember(myStatus.uid) {
//        MarkerState(position = LatLng(myStatus.latitude, myStatus.longitude))
//    }

//    LaunchedEffect(myStatus.latitude, myStatus.longitude) {
//        myMarkerState.position = LatLng(myStatus.latitude, myStatus.longitude)
//    }

    LaunchedEffect(profileImageUrl) {
        myCustomMarkerIcon =
            createCustomMarkerBitmap(context = context, imageUrl = profileImageUrl, isOnline = true)
    }

//    if (myCustomMarkerIcon != null) {
//        Marker(state = myMarkerState, icon = myCustomMarkerIcon, zIndex = 0.0f, onClick = { true })
//    }

    // 비트맵 상태(null -> 완성)가 바뀔 때 구글 맵이 마커를 강제로 다시 그리도록 key 지정
    key(myStatus.uid, myCustomMarkerIcon) {
        if (myCustomMarkerIcon != null) {

            val myMarkerState = remember(myStatus.uid, myStatus.latitude, myStatus.longitude) {
                MarkerState(position = LatLng(myStatus.latitude, myStatus.longitude))
            }

            Marker(
                state = myMarkerState,
                icon = myCustomMarkerIcon,
                zIndex = 0.0f,
                onClick = { true }
            )
        }
    }
}

@Composable
private fun FriendMarker(friend: LiveStatus, onFriendClick: (LiveStatus) -> Unit) {
    val context = LocalContext.current
    var friendCustomMarkerIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

//    val friendMarkerState = remember(friend.uid) {
//        MarkerState(position = LatLng(friend.latitude, friend.longitude))
//    }

//    LaunchedEffect(friend.latitude, friend.longitude) {
//        friendMarkerState.position = LatLng(friend.latitude, friend.longitude)
//    }

    LaunchedEffect(friend.profileImageUrl, friend.isOnline) {
        friendCustomMarkerIcon = createCustomMarkerBitmap(
            context = context,
            imageUrl = friend.profileImageUrl,
            isOnline = friend.isOnline
        )
    }

    // 프로필 비트맵이 완전히 준비되었을 때만 지도에 마커를 등록하고 업데이트 유발
    key(friend.uid, friendCustomMarkerIcon) {
        if (friendCustomMarkerIcon != null) {
            val friendMarkerState = remember(friend.uid, friend.latitude, friend.longitude) {
                MarkerState(position = LatLng(friend.latitude, friend.longitude))
            }

            Marker(
                state = friendMarkerState,
                icon = friendCustomMarkerIcon,
                zIndex = 1.0f,
                onClick = {
                    onFriendClick(friend)
                    true
                }
            )
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun HistoryMarker(history: History, onHistoryClick: (History) -> Unit) {
    val historyLatLng = remember(history.latitude, history.longitude) {
        LatLng(history.latitude, history.longitude)
    }
    val markerState = remember(history.id) { MarkerState(position = historyLatLng) }

    LaunchedEffect(history.latitude, history.longitude) {
        markerState.position = historyLatLng
    }

    val bitmapKey = remember(history.id, history.category) { "${history.id}_${history.category}" }
    val (iconResId, bgColor, iconColor) = remember(history.category) {
        when (history.category) {
            "무전" -> Triple(R.drawable.ic_walkie_talkie_icon, Color(0xFFFAF5FF), Color(0xFFA855F7))
            "카페" -> Triple(R.drawable.ic_cafe_icon, Color(0xFFFFFBEB), Color(0xFFD97706))
            "음식" -> Triple(R.drawable.ic_restaurant_icon, Color(0xFFFFF1F2), Color(0xFFF43F5E))
            "운동" -> Triple(R.drawable.ic_exercise_icon, Color(0xFFECFDF5), Color(0xFF10B981))
            else -> Triple(R.drawable.ic_daily_icon, Color(0xFFEEF2FF), Color(0xFF6366F1))
        }
    }

    val composeMarkerBitmap = rememberComposeBitmapDescriptor(bitmapKey, bitmapKey) {
        HistoryIconCircle(iconResId = iconResId, iconTint = iconColor, backgroundColor = bgColor)
    }

    Marker(
        state = markerState,
        title = "[${history.category}] ${history.placeName}",
        snippet = "${history.userNickname}: ${history.content}",
        icon = composeMarkerBitmap,
        alpha = 0.95f,
        zIndex = 2.0f,
        onClick = {
            onHistoryClick(history)
            true
        }
    )
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
            containerColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = "친구 목록 열기",
            modifier = Modifier.size(24.dp),
            tint = primary
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
                colors = CardDefaults.cardColors(containerColor = subBackground),
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
                        tint = Color.Gray.copy(alpha = 0.6f),
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
                                            color = if (friend.isOnline) online else Color.Gray,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = friend.nickname,
                            style = Typography.bodyMedium,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = friend.status.ifEmpty { "등록된 한줄 메세지가 없습니다" },
                                style = Typography.bodyMedium,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
//                                overflow = TextOverflow.Ellipsis
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
                                    containerColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 1.dp
                                )
                            ) {
                                Text(
                                    text = "채팅",
                                    fontWeight = FontWeight.Bold,
                                    style = Typography.bodyMedium
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
                                            voiceUiState.isRecording -> recording
                                            voiceUiState.isUploading -> send
                                            else -> primary
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
                                    style = Typography.bodyMedium,
                                    color = Color.White
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
                .background(subBackground.copy(alpha = 0.1f)),
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
                                tint = primary
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
        targetValue = if (isSharingEnabled) online else Color.Gray,
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
                style = Typography.bodyMedium,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSharingEnabled) fontDefault else Color.DarkGray,
                letterSpacing = (-0.3).sp
            )
        }
    }
}