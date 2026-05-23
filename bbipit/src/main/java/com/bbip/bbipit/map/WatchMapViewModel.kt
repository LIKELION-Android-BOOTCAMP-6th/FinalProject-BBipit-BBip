package com.bbip.bbipit.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.base.WatchBaseViewModel
import com.bbip.bbipit.models.WatchLiveStatus
import com.bbip.bbipit.service.WatchCentralService
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 지도 UI 상태 데이터 클래스
 */
data class WatchMapUiState(
    val locationList: List<WatchLiveStatus> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * 워치 지도 상태 관리 뷰모델
 */
class WatchMapViewModel: WatchBaseViewModel<WatchMapUiState>(WatchMapUiState()) {

    init {
        // 위치 이벤트 버스 구독 및 상태 업데이트
        viewModelScope.launch {
            WatchCentralService.locationEventBus.collect { decryptedList ->
                updateState {
                    copy(locationList = decryptedList)
                }
            }
        }
    }
}