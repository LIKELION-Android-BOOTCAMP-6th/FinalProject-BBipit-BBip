package com.bbip.bbipit.presentation.map.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BackgroundListenerService
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.data.repository.ChatRepositoryImpl
import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 기존 UI 구조 명세 완벽 충족 목적의 최적화 완료 지도 매핑 아키텍처 상태 구조 클래스
 */
data class MapUiState(
    val myStatus: LiveStatus? = null,
    val friendsStatuses: List<LiveStatus> = emptyList(),
    val isLoading: Boolean = true,
    val isLocationSharing: Boolean = true,
    val isStopSharingDialogShown: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val liveStatusRepository: LiveStatusRepository,
    private val chatRepository: ChatRepositoryImpl,
): BaseViewModel<MapUiState>(MapUiState()) {
    private val myLiveStatus = MutableStateFlow<LiveStatus?>(null)
    private val TAG = "MapViewModel"

    init {
        observeLiveStatusStreams()
    }

    /**
     * 친구와의 1:1 채팅방 생성 또는 기존 방 ID 가져오기
     */
    fun createOrGetChatRoom(
        targetUid: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            chatRepository.createOrGetChatRoom(targetUid)
                .onSuccess { result ->
                    if (result.success && result.roomId != null) {
                        onSuccess(result.roomId)
                    } else {
                        onError(result.message.ifEmpty { "채팅방 ID를 가져올 수 없습니다." })
                    }
                }
                .onFailure { error ->
                    onError(error.message ?: "채팅방 생성 중 오류가 발생했습니다.")
                }
        }
    }

//    fun fetchLiveStatusAndRefreshCache() {
//        viewModelScope.launch {
//
//            liveStatusRepository.refreshMyLiveStatusCache().onSuccess {
//                Log.d(TAG, it)
//            }.onFailure {
//                Log.d(TAG, "${it.message}")
//            }
//        }
//    }

    /**
     * Repository 관찰 흐름을 ViewModel의 상태 구조와 연결
     */
    private fun observeLiveStatusStreams() {
        viewModelScope.launch {
            combine(
                liveStatusRepository.myLiveStatusFlow,
                liveStatusRepository.friendsLiveStatusFlow,
                liveStatusRepository.observeLocationSharingState(),
                myLiveStatus
            ) { myStatus, friendsStatuses, isSharingEnabled, cacheStatus ->
                val currentMyStatus = myStatus ?: cacheStatus
                Triple(currentMyStatus, friendsStatuses, isSharingEnabled)
            }.collectLatest { (currentMyStatus, friendsStatuses, isSharingEnabled) ->
                updateState {
                    copy(
                        myStatus = currentMyStatus,
                        friendsStatuses = friendsStatuses,
                        isLoading = currentMyStatus == null,
                        isLocationSharing = isSharingEnabled
                    )
                }
            }
        }
    }

    // 토글 버튼 클릭 시 호출할 함수
    fun toggleLocationSharing(isEnabled: Boolean, context: Context) {
        viewModelScope.launch {
            liveStatusRepository.updateLocationSharingState(isEnabled)
                .onSuccess {
                    Log.d(TAG, "위치 공유 상태 변경 성공: $isEnabled")
                    if (isEnabled) {
                        updateCurrentLocation(context)
                    }
                }
                .onFailure {
                    Log.e(TAG, "위치 공유 상태 변경 실패")
                }
        }
    }

    fun updateCurrentLocation(context: Context) {
        val intent = Intent(context, BackgroundListenerService::class.java).apply {
            // 서비스가 어떤 행동을 해야 하는지 식별할 액션값
            action = BackgroundListenerService.ACTION_REQUEST_SINGLE_LOCATION_UPDATE
        }
        context.startService(intent)
    }

    fun onUpdateStopSharingDialog(value : Boolean) = updateState { copy(isStopSharingDialogShown = value) }
}