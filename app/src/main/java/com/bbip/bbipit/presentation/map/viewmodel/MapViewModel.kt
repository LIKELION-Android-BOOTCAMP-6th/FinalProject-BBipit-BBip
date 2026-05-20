package com.bbip.bbipit.presentation.map.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 기존 UI 구조 명세 완벽 충족 목적의 최적화 완료 지도 매핑 아키텍처 상태 구조 클래스
 */
data class MapUiState(
    val myStatus: LiveStatus? = null,
    val friendsStatuses: List<LiveStatus> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val liveStatusRepository: LiveStatusRepository,
    private val fusedLocationClient: FusedLocationProviderClient
): ViewModel() {

    // 서버 데이터 공급 전 로컬 캐시 레이어 즉시 파싱용 백업용 Flow
    private val cacheMyStatus = MutableStateFlow<LiveStatus?>(null)

    init {
        // 인스턴스 초기화 즉시 기기 캐시 기반 마지막 동선 확보 가동
        fetchLastKnownLocation()
    }

    /**
     * 리포지토리 실시간 Flow 및 기기 내부 캐시 상태 유기적 조합(Combine) 반응형 파이프라인 변수
     */
    val uiState: StateFlow<MapUiState> = combine(
        liveStatusRepository.myLiveStatusFlow,
        liveStatusRepository.friendsLiveStatusFlow,
        cacheMyStatus
    ) { myStatus, friendsStatuses, cacheStatus ->

        // 원격 서버 데이터 지연 시 유저 경험 방어 목적의 즉각적 로컬 캐시 값 대체 처리
        val currentMyStatus = myStatus ?: cacheStatus

        MapUiState(
            myStatus = currentMyStatus,
            friendsStatuses = friendsStatuses,
            // 로컬 및 서버 데이터 실체성 확보 시점 기준 로딩 프로그래스 중단 처리
            isLoading = currentMyStatus == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    /**
     * Google Fused Location 서비스 인프라 활용 기반 원격 데이터 수신 지연 현상 방어 최적화 함수
     */
    @SuppressLint("MissingPermission")
    private fun fetchLastKnownLocation() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            try {
                // 캐시 위치 가져오기 선제 시도
                var lastLocation = fusedLocationClient.lastLocation.await()

                // 신규 가입 등으로 캐시 부재 시 신선한 현재 위치 단발성 요청 (Priority.PRIORITY_HIGH_ACCURACY)
                if (lastLocation == null) {
                    Log.d("MapViewModel", "캐시 위치가 없으므로 실시간 단발성 위치(CurrentLocation)를 조회합니다.")
                    val locationRequest = CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build()
                    lastLocation = fusedLocationClient.getCurrentLocation(locationRequest, null).await()
                }

                if (lastLocation != null) {
                    Log.d("MapViewModel", "🎯 초기 위치 확보 성공: ${lastLocation.latitude}, ${lastLocation.longitude}")
                    cacheMyStatus.value = LiveStatus(
                        uid = uid ?: "unknown_me",
                        latitude = lastLocation.latitude,
                        longitude = lastLocation.longitude
                    )
                } else {
                    // 극단적 음영 지역 혹은 GPS 비활성화 시 무한 로딩 방지 목적의 서울시청 임시 좌표 부여
                    Log.w("MapViewModel", "⚠️ 모든 위치 조회 실패: 무한 로딩 방지를 위해 기본 앵커를 설정합니다.")
                    cacheMyStatus.value = LiveStatus(
                        uid = uid ?: "unknown_me",
                        latitude = 37.5665,
                        longitude = 126.9780
                    )
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "최근 위치 획득 실패 예외 발생: ${e.message}")
                // 예외 발생 시 무한 인디케이터 방어용 임시 좌표 설정
                cacheMyStatus.value = LiveStatus(
                    uid = uid ?: "unknown_me",
                    latitude = 37.5665,
                    longitude = 126.9780
                )
            }
        }
    }
}