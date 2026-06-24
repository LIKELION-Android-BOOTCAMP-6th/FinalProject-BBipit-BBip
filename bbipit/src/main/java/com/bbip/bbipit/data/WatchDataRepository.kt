package com.bbip.bbipit.data

import com.bbip.bbipit.models.WatchLiveStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 워치 앱 전체에서 공유되는 전역 실시간 데이터 저장소 (싱글톤)
 */
object WatchDataRepository {
    private val _liveStatusList = MutableStateFlow<List<WatchLiveStatus>>(emptyList())
    val liveStatusList: StateFlow<List<WatchLiveStatus>> = _liveStatusList.asStateFlow()

    private val _historyList = MutableStateFlow<List<WatchHistory>>(emptyList())
    val historyList: StateFlow<List<WatchHistory>> = _historyList

    /**
     * 히스토리 패킷을 수신 처리
     *  JSON 배열 데이터를 받아 상태 업데이트
     */
    fun updateHistoriesFromJson(jsonString: String) {
        try {
            val type = object : TypeToken<List<WatchHistory>>() {}.type
            val histories: List<WatchHistory> = Gson().fromJson(jsonString, type)

            _historyList.value = histories
        } catch (e: Exception) {
            _historyList.value = emptyList()
        }
    }

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