package com.bbip.bbipit.data

import com.bbip.bbipit.models.WatchLiveStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 워치 앱 전체에서 공유되는 전역 실시간 데이터 저장소 (싱글톤)
 */
object WatchDataRepository {
    private val TAG = "WatchDataRepository"

    // 🎯 친구들의 실시간 위치 목록을 보관하는 StateFlow
    // 싱글톤이므로 메모리에 항상 최신 상태가 유지됩니다.
    private val _liveStatusList = MutableStateFlow<List<WatchLiveStatus>>(emptyList())
    val liveStatusList: StateFlow<List<WatchLiveStatus>> = _liveStatusList.asStateFlow()

    /**
     * 패킷을 수신했을 때 최신 위치 데이터를 업데이트하는 함수
     */
    fun updateLocationList(newList: List<WatchLiveStatus>) {
        _liveStatusList.value = newList
    }

    /**
     * 필요 시 데이터를 초기화하는 함수 (예: 로그아웃, 앱 종료 시)
     */
    fun clearData() {
        _liveStatusList.value = emptyList()
    }
}