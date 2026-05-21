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
 * 실시간 위치 확인 및 무전 기능을 통합 제공하는 워치 지도 화면 컴포저블
 */
@Composable
fun WatchMapScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 데이터 스트림 및 상태 관리를 위한 뷰모델 인스턴스 획득
    val mapViewModel: WatchMapViewModel = viewModel()
    val locationList by mapViewModel.locationList.collectAsState()

    // 의존성 주입 팩토리를 통한 무전 기능 전용 뷰모델 인스턴스 획득
    val voiceViewModel: WatchVoiceViewModelWatch = viewModel(
        factory = WatchVoiceViewModelWatch.provideFactory(context)
    )

    // 최초 지도 로딩 기준점 설정을 위한 서울 좌표 및 카메라 상태 정의
    val seoul = LatLng(37.5665, 126.9780)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(seoul, 15f)
    }

    // 상세 프로필 팝업 표출 여부를 제어하는 선택된 친구 데이터 상태
    var clickedFriend by remember { mutableStateOf<WatchLiveStatus?>(null) }

    // 중복 연산 방지 및 상태별 테두리 갱신을 위한 유저별 커스텀 마커 이미지 캐시 상태
    var markerDescriptors by remember { mutableStateOf<Map<String, BitmapDescriptor>>(emptyMap()) }

    // 화면 진입 초기 시점의 모바일 기기 대상 권한 동기화 및 위치 데이터 즉시 갱신 요청
    LaunchedEffect(Unit) {
        sendPermissionRequestToPhone(context)
        requestImmediateLocationSync(context)
    }

    // 최초 1회에 한해 내 위치 중심으로 지도의 카메라를 부드럽게 이동시키는 초기화 루틴
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

    // 유저 리스트 혹은 실시간 접속 상태 변경 시 마커용 프로필 이미지를 비동기로 생성 및 캐싱하는 루틴
    LaunchedEffect(locationList) {
        val updatedDescriptors = markerDescriptors.toMutableMap()

        locationList.forEach { userStatus ->
            // 상태 변경에 따른 테두리 색상 갱신을 위해 닉네임과 온라인 여부를 결합한 고유 식별 키 정의
            val cacheKey = "${userStatus.nickname}_${userStatus.isOnline}"

            // 미등록 마커에 대한 비동기 그래픽 소스 로드 및 비트맵 변환 처리
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
        // 구글 맵 컴포넌트 배치 및 속성 설정
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
        ) {
            // 실시간 수집된 위치 데이터 기반의 마커 드로잉 반복 루프
            locationList.forEachIndexed { index, userStatus ->
                val cacheKey = "${userStatus.nickname}_${userStatus.isOnline}"

                // 캐시 비트맵 유무를 확인하고 로딩 시점에는 인덱스 및 상태별 시스템 기본 마커로 대체
                val markerIcon = markerDescriptors[cacheKey]
                    ?: BitmapDescriptorFactory.defaultMarker(
                        if (index == 0) BitmapDescriptorFactory.HUE_AZURE // 본인 위치 마커 색상
                        else if (userStatus.isOnline) BitmapDescriptorFactory.HUE_GREEN // 온라인 친구 마커 색상
                        else BitmapDescriptorFactory.HUE_RED // 오프라인 친구 마커 색상
                    )

                Marker(
                    state = rememberMarkerState(
                        position = LatLng(userStatus.latitude, userStatus.longitude)
                    ).apply {
                        position = LatLng(userStatus.latitude, userStatus.longitude)
                    },
                    title = userStatus.nickname,
                    icon = markerIcon, // 커스텀 변환 비트맵 아이콘 적용
                    onClick = {
                        // 타인 마커 선택 시에만 프로필 상세 다이얼로그 활성화 처리
                        if (index != 0) {
                            clickedFriend = userStatus
                        }
                        true
                    }
                )
            }
        }

        // 친구 마커 선택 시 해당 사용자 상세 프로필 및 무전 버튼 팝업 가시화
        clickedFriend?.let { friend ->
            WatchFriendProfileDialog(
                friend = friend,
                onDismiss = { clickedFriend = null },
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
 * 백그라운드 캐시 갱신 유도를 위해 연결된 연동 스마트폰 기기로 위치 정보 즉시 동기화 신호 송신
 */
suspend fun requestImmediateLocationSync(context: Context) {
    try {
        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        // 현재 블루투스 및 네트워크로 연결된 모든 웨어러블 노드 검색
        val nodes = nodeClient.connectedNodes.await()

        // 검색된 첫 번째 매칭 노드를 스마트폰으로 간주하여 고유 식별자 추출 후 동기화 명령 송신
        nodes.firstOrNull()?.id?.let { targetNodeId ->
            messageClient.sendMessage(
                targetNodeId,
                "/request_friends_location",
                byteArrayOf() // 단순 트리거 목적의 데이터가 없는 단발성 페이로드 전달
            ).await()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 모바일 기기의 위치 수집 엔진 및 백그라운드 프로세스 활성화를 위한 동기화 신호 송신
 */
suspend fun sendPermissionRequestToPhone(context: Context) {
    val nodeClient = Wearable.getNodeClient(context)
    val messageClient = Wearable.getMessageClient(context)

    try {
        val nodes = nodeClient.connectedNodes.await()
        val targetNodeId = nodes.firstOrNull()?.id

        // 연동된 스마트폰 식별자 유효성 확인 후 모바일 전용 권한 팝업 가이드 유도 메시지 송신
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