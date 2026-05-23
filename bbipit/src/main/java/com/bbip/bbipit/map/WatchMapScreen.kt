package com.bbip.bbipit.map

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bbip.bbipit.base.WatchFriendProfileDialog
import com.bbip.bbipit.base.WatchVoiceViewModelWatch
import com.bbip.bbipit.base.WatchPushToTalkButton
import com.bbip.bbipit.models.WatchLiveStatus
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

import android.content.Context
import com.bbip.bbipit.base.WatchIncomingVoiceDialog
import com.bbip.bbipit.base.createCustomMarkerBitmap
import com.bbip.bbipit.models.WatchVoiceData
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * 워치 지도 및 무전 통합 화면 컴포저블
 */
@Composable
fun WatchMapScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 뷰모델 인스턴스 획득
    val mapViewModel: WatchMapViewModel = viewModel()
    val uiState by mapViewModel.uiState.collectAsState()

    val locationList = uiState.locationList

    // 무전 뷰모델 팩토리 주입 및 인스턴스 획득
    val voiceViewModel: WatchVoiceViewModelWatch = viewModel(
        factory = WatchVoiceViewModelWatch.provideFactory(context)
    )

    // 서울 초기 좌표 및 카메라 상태 정의
    val seoul = LatLng(37.5665, 126.9780)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(seoul, 15f)
    }

    // 선택된 친구 데이터 상태
    var clickedFriend by remember { mutableStateOf<WatchLiveStatus?>(null) }

    // 마커 이미지 캐시 상태
    var markerDescriptors by remember { mutableStateOf<Map<String, BitmapDescriptor>>(emptyMap()) }

    // 초기 권한 요청 및 위치 동기화 신호 송신
    LaunchedEffect(Unit) {
        sendPermissionRequestToPhone(context)
        requestImmediateLocationSync(context)
    }

    // 내 위치 중심 카메라 초기 이동 처리
    var isCameraInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(locationList) {
        if (locationList.isNotEmpty() && !isCameraInitialized) {
            val myTargetLocation = locationList.first()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(myTargetLocation.latitude, myTargetLocation.longitude), 15.5f
                ),
                durationMs = 600
            )
            isCameraInitialized = true
        }
    }

    // 위치 리스트 변경 시 마커 이미지 생성 및 캐싱
    LaunchedEffect(locationList) {
        val updatedDescriptors = markerDescriptors.toMutableMap()

        locationList.forEach { userStatus ->
            // 닉네임과 온라인 여부를 조합한 캐시 키 정의
            val cacheKey = "${userStatus.nickname}_${userStatus.isOnline}"

            // 미등록 마커 이미지 비동기 생성
            if (!updatedDescriptors.containsKey(cacheKey)) {
                val bitmapDescriptor = createCustomMarkerBitmap(
                    context = context,
                    imageUrl = userStatus.profileImageUrl ?: "",
                    isOnline = userStatus.isOnline
                )
                updatedDescriptors[cacheKey] = bitmapDescriptor
            }
        }
        markerDescriptors = updatedDescriptors
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 구글 맵 컴포넌트
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
        ) {
            // 위치 데이터 기반 마커 표시
            locationList.forEachIndexed { index, userStatus ->
                val cacheKey = "${userStatus.nickname}_${userStatus.isOnline}"

                // 캐시 이미지 없을 경우 상태별 기본 마커 적용
                val markerIcon = markerDescriptors[cacheKey]
                    ?: BitmapDescriptorFactory.defaultMarker(
                        if (index == 0) BitmapDescriptorFactory.HUE_AZURE // 본인 마커
                        else if (userStatus.isOnline) BitmapDescriptorFactory.HUE_GREEN // 온라인 친구 마커
                        else BitmapDescriptorFactory.HUE_RED // 오프라인 친구 마커
                    )

                Marker(
                    state = rememberMarkerState(
                        position = LatLng(userStatus.latitude, userStatus.longitude)
                    ).apply {
                        position = LatLng(userStatus.latitude, userStatus.longitude)
                    },
                    title = userStatus.nickname,
                    icon = markerIcon,
                    onClick = {
                        // 타인 마커 클릭 시 프로필 다이얼로그 활성화
                        if (index != 0) {
                            clickedFriend = userStatus
                        }
                        true
                    }
                )
            }
        }

        // 친구 프로필 상세 다이얼로그 노출 및 무전 버튼 연결
        clickedFriend?.let { friend ->
            LaunchedEffect(friend.uid) {
                // 다이얼로그 활성화 중 타겟 UID 바인딩 유지
                voiceViewModel.setTargetUid(friend.uid)
            }
            WatchFriendProfileDialog(
                friend = friend,
                onDismiss = {
                    // 다이얼로그 종료 시 타겟 UID 초기화
                    voiceViewModel.setTargetUid(null)
                    clickedFriend = null
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

/**
 * 모바일 기기로 친구 위치 정보 즉시 동기화 신호 송신
 */
suspend fun requestImmediateLocationSync(context: Context) {
    try {
        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        // 연결된 웨어러블 노드 검색
        val nodes = nodeClient.connectedNodes.await()

        // 첫 번째 노드(스마트폰)로 위치 갱신 명령 전달
        nodes.firstOrNull()?.id?.let { targetNodeId ->
            messageClient.sendMessage(
                targetNodeId,
                "/request_friends_location",
                byteArrayOf()
            ).await()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 모바일 기기로 권한 요청 가이드 신호 송신
 */
suspend fun sendPermissionRequestToPhone(context: Context) {
    val nodeClient = Wearable.getNodeClient(context)
    val messageClient = Wearable.getMessageClient(context)

    try {
        val nodes = nodeClient.connectedNodes.await()
        val targetNodeId = nodes.firstOrNull()?.id

        // 연결된 노드 유효성 확인 후 권한 안내 메시지 전달
        if (targetNodeId != null) {
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