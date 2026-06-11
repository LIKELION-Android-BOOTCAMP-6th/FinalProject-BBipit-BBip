package com.bbip.bbipit.map

import android.util.Log
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.Icon
import com.bbip.bbipit.base.WatchFriendProfileDialog
import com.bbip.bbipit.base.WatchVoiceViewModel
import com.bbip.bbipit.base.WatchPushToTalkButton
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.bbip.bbipit.base.createCustomMarkerBitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.maps.android.compose.MapUiSettings
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import com.bbip.bbipit.R
import com.bbip.bbipit.base.createHistoryMarkerBitmap
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties
import kotlinx.coroutines.launch

@Composable
fun WatchMapScreen(
    modifier: Modifier = Modifier
) {
    val TAG = "WatchMapScreen"
    val context = LocalContext.current
    val viewModel: WatchMapViewModel = viewModel()

    val uiState by viewModel.uiState.collectAsState()
    val liveStatusList = uiState.liveStatusList
    val myLocation = uiState.liveStatusList.firstOrNull() // 리스트의 첫 번째가 '나'의 상태

    val historyList = uiState.historyList
    // 마커 그래픽 중복 생성 방지용 캐시 맵
    var historyDescriptors by remember { mutableStateOf<Map<String, BitmapDescriptor>>(emptyMap()) }
    // 클릭된 히스토리 상태 관리를 위한 변수
    var clickedHistory by remember { mutableStateOf<com.bbip.bbipit.data.WatchHistory?>(null) }
    var showHistories by remember { mutableStateOf(true) }

    val voiceViewModel: WatchVoiceViewModel = viewModel(
        factory = WatchVoiceViewModel.provideFactory(context)
    )

    val seoul = LatLng(37.5665, 126.9780)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(seoul, 15f)
    }

    var isCameraInitialized by remember { mutableStateOf(false) }

    // ✅ 마커 비트맵 캐시 상태를 상단에 단 하나만 선언
    var markerDescriptors by remember { mutableStateOf<Map<String, BitmapDescriptor>>(emptyMap()) }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 드로어의 상태를 MapScreen으로 끌어올림
    val drawerWidth = 150.dp
    val drawerWidthPx = with(density) { drawerWidth.toPx() }

    val drawerDraggableState = remember {
        AnchoredDraggableState<DrawerState>(
            initialValue = DrawerState.CLOSED,
            positionalThreshold = { distance: Float -> distance * 0.4f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(durationMillis = 300),
            decayAnimationSpec = exponentialDecay()
        )
    }

    // 리스트 유입 시 카테고리별 마커 비트맵 캐싱
    LaunchedEffect(historyList) {
        val updatedDescriptors = historyDescriptors.toMutableMap()
        var isUpdated = false

        historyList.forEach { history ->
            if (!updatedDescriptors.containsKey(history.category)) {
                updatedDescriptors[history.category] = createHistoryMarkerBitmap(context, history.category)
                isUpdated = true
            }
        }
        if (isUpdated) {
            historyDescriptors = updatedDescriptors
        }
    }

    // 드로어 앵커 초기화
    LaunchedEffect(drawerWidthPx) {
        val newAnchors = DraggableAnchors<DrawerState> {
            DrawerState.CLOSED at -drawerWidthPx
            DrawerState.OPENED at 0f
        }
        drawerDraggableState.updateAnchors(newAnchors)
    }

    // 내 위치 중심 카메라 초기 이동 처리
    LaunchedEffect(liveStatusList) {
        if (myLocation != null && !isCameraInitialized && uiState.selectedFriendUid == null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(myLocation.latitude, myLocation.longitude), 15.5f
                ),
                durationMs = 500
            )
            isCameraInitialized = true
        }
    }

    LaunchedEffect(myLocation?.latitude, myLocation?.longitude) {
        if (myLocation != null && uiState.selectedFriendUid == null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(myLocation.latitude, myLocation.longitude),
                    15.5f
                ),
                durationMs = 500 // 이동 애니메이션 속도
            )
        }
    }

    // 리스트가 변경될 때 마커 비트맵을 '딱 한 번만' 비동기 생성 및 캐싱
    LaunchedEffect(liveStatusList) {
        val updatedDescriptors = markerDescriptors.toMutableMap()
        var isUpdated = false

        liveStatusList.forEach { userStatus ->
            // 이미지 URL과 온라인 상태가 같으면 동일 마커로 취급 (위치 변화로 인한 재생성 차단)
            val cacheKey = "${userStatus.profileImageUrl}_${userStatus.isOnline}"

            if (!updatedDescriptors.containsKey(cacheKey)) {
                val bitmapDescriptor = createCustomMarkerBitmap(
                    context = context,
                    imageUrl = userStatus.profileImageUrl,
                    isOnline = userStatus.isOnline
                )
                updatedDescriptors[cacheKey] = bitmapDescriptor
                isUpdated = true
            }
        }

        // 실제로 새로운 마커가 추가되었을 때만 상태 업데이트를 유발하여 Recomposition 방지
        if (isUpdated) {
            markerDescriptors = updatedDescriptors
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = MapProperties(mapStyleOptions
            = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)),
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
            cameraPositionState = cameraPositionState,
        ) {
            liveStatusList.forEachIndexed { index, userStatus ->
                val cacheKey = "${userStatus.profileImageUrl}_${userStatus.isOnline}"
                val customMarkerIcon = markerDescriptors[cacheKey]

                // rememberMarkerState에 고유 UID를 Key로 지정하여 위치 변화 추적 안정화
                val markerState = rememberMarkerState(
                    key = userStatus.uid,
                    position = LatLng(userStatus.latitude, userStatus.longitude)
                )

                // 위치 데이터가 변경되면 마커의 position 속성만 자연스럽게 업데이트
                LaunchedEffect(userStatus.latitude, userStatus.longitude) {
                    markerState.position = LatLng(userStatus.latitude, userStatus.longitude)
                }

                if (customMarkerIcon != null) {
                    Marker(
                        state = markerState,
                        title = userStatus.nickname,
                        icon = customMarkerIcon,
                        zIndex = 1.0f,
                        onClick = {
                            if (index != 0) {
                                viewModel.selectFriend(userStatus.uid)
                            }
                            false
                        }
                    )
                }
            }

            // 실시간 공유된 간소화 발자취 마커 그리기 추가
            if (showHistories) {
                historyList.forEach { history ->
                    val customIcon = historyDescriptors[history.category]

                    val markerState = rememberMarkerState(
                        key = history.id, // 문서 ID 기준으로 상태 추적 고정
                        position = LatLng(history.latitude, history.longitude)
                    )

                    if (customIcon != null) {
                        Marker(
                            state = markerState,
                            icon = customIcon,
                            zIndex = 2.0f,
                            onClick = {
                                // 마커 클릭 시 다이얼로그 상태를 활성화
                                clickedHistory = history
                                true
                            }
                        )
                    }
                }
            }
        }

        clickedHistory?.let { history ->
            WatchHistoryDetailDialog(
                history = history,
                onDismiss = { clickedHistory = null },
                onOpenOnPhoneClick = {
                    // 사용자가 팝업 내부의 버튼을 눌렀을 때 진짜 휴대폰 연동 파이프라인 가동
                    viewModel.requestOpenHistoryOnPhone(context, history.id)
                    clickedHistory = null // 다이얼로그 닫기
                }
            )
        }

        // 상태 변수들을 Box 스코프 상단에 올바르게 배치하고 들여쓰기 수정
        val isFriendSelected = uiState.selectedFriendUid != null
        val isDrawerOpeningOrOpened = drawerDraggableState.targetValue == DrawerState.OPENED

        // 지도보다 먼저 이벤트를 가로챌 왼쪽 가장자리 투명 터치 패널
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(26.dp) // 에지 터치를 감지할 최적의 범위
                .align(Alignment.CenterStart)
                .anchoredDraggable(
                    state = drawerDraggableState,
                    orientation = Orientation.Horizontal,
                    // 친구 다이얼로그가 떴거나, 드로어가 이미 열려있는 상태라면 에지 스와이프 차단
                    enabled = !isFriendSelected && !isDrawerOpeningOrOpened
                )
                .zIndex(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (!isFriendSelected && !isDrawerOpeningOrOpened) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight(0.3f)
                        .background(
                            color = Color(0xFF956AFC),
                            shape = RoundedCornerShape(
                                topEnd = 8.dp,
                                bottomEnd = 8.dp
                            )
                        )
                )
            }
        }

        Button(
            onClick = { showHistories = !showHistories },
            modifier = Modifier
                .align(Alignment.CenterEnd) // 내 위치 버튼과 같은 라인
                .padding(end = 16.dp, bottom = 90.dp) // 내 위치 버튼(44dp + 간격) 위로 올림
                .size(44.dp)
                .border(
                    width = 2.dp,
                    color = if (!showHistories) Color(0xFF956AFC) else Color.Gray,
                    shape = CircleShape
                ),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFF1F5F9)
            )
        ) {
            Icon(
                painter = if (showHistories) painterResource(id = R.drawable.ic_footprint_icon_hidden)
                else painterResource(id = R.drawable.ic_footprint_icon_hidden),
                contentDescription = "히스토리 토글",
                modifier = Modifier.size(28.dp),
                tint = if (!showHistories) Color(0xFF956AFC) else Color.Gray
            )
        }

        // 내 위치 이동 버튼
        Button(
            onClick = {
                viewModel.refreshCurrentLocationAndSync(context)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd) // 우측 중앙 정렬
                .padding(end = 8.dp, top = 10.dp)
                .size(44.dp)
                .border(
                    width = 2.dp,
                    color = Color(0xFF956AFC),
                    shape = CircleShape
                ),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFF1F5F9) // 배경색
            )
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "내 위치",
                modifier = Modifier.size(22.dp),
                tint = Color(0xFF956AFC) // 보라색 포인트
            )
        }

        // WearOS 전용 에지 스와이프 친구 추적 드로어 배치
        WatchFriendListDrawer(
            draggableState = drawerDraggableState, // 주입식으로 변경한 상태를 바인딩
            friends = liveStatusList,
            selectedFriendUid = uiState.selectedFriendUid,
            drawerWidth = drawerWidth,
            onFriendClick = { friend ->
                viewModel.selectFriend(friend.uid)
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLng(
                        LatLng(
                            friend.latitude,
                            friend.longitude
                        )
                    )
                )
                coroutineScope.launch {
                    drawerDraggableState.animateTo(DrawerState.CLOSED)
                }
            }
        )

        val currentClickedFriend = remember(uiState.selectedFriendUid, uiState.liveStatusList) {
            uiState.liveStatusList.find { it.uid == uiState.selectedFriendUid }
        }

        // 친구 프로필 상세 다이얼로그 노출 및 무전 버튼 연결
        currentClickedFriend?.let { friend ->
            LaunchedEffect(friend.uid) {
                voiceViewModel.setTargetUid(friend.uid)
            }
            WatchFriendProfileDialog(
                friend = friend,
                onDismiss = {
                    Log.d(TAG, "다이얼로그 컴포저블 화면 onDismiss")
                    voiceViewModel.setTargetUid(null)
                    viewModel.selectFriend(null)
                },
                walkieTalkieButton = {
                    WatchPushToTalkButton(
                        viewModel = voiceViewModel,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            )
        }
    }
}