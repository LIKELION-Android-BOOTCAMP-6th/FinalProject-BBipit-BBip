package com.bbip.bbipit.map

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.base.WatchBaseViewModel
import com.bbip.bbipit.models.WatchLiveStatus
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.Context
import com.bbip.bbipit.data.WatchDataRepository
import com.bbip.bbipit.data.WatchHistory

/**
 * 지도 UI 상태 데이터 클래스
 */
data class WatchMapUiState(
    val liveStatusList: List<WatchLiveStatus> = emptyList(),
    val historyList: List<WatchHistory> = emptyList(),
    val selectedFriendUid: String? = null,
    val isLoading: Boolean = false
)

/**
 * 워치 지도 상태 관리 뷰모델
 */
class WatchMapViewModel:
    WatchBaseViewModel<WatchMapUiState>(WatchMapUiState())
{
    val TAG = "WatchMapViewModel"

    companion object {
        // 워치에서 특정 히스토리 클릭 시 휴대폰에 뷰어를 열도록 요청하는 경로
        const val PATH_REQUEST_OPEN_HISTORY = "/request_open_history"
    }

    init {
        // 위치 이벤트 버스 구독 및 상태 업데이트
        viewModelScope.launch {
            WatchDataRepository.liveStatusList.collect { decryptedList ->
                updateState {
                    copy(liveStatusList = decryptedList)
                }
                Log.d(TAG, "위치 이벤트 버스를 통해 라이브 리스트 상태 업데이트")
            }
        }

        // 히스토리 데이터 구독 추가
        viewModelScope.launch {
            WatchDataRepository.historyList.collect { histories ->
                updateState { copy(historyList = histories) }
                Log.d(TAG, "👣 워치 히스토리 데이터 동기화 완료: ${histories.size}건")
            }
        }
    }

    /**
     * 워치에서 히스토리 마커 클릭 시 휴대폰으로 문서 ID 전송
     */
    fun requestOpenHistoryOnPhone(context: Context, historyId: String) {
        val messageClient = Wearable.getMessageClient(context)
        val nodeClient = Wearable.getNodeClient(context)

        viewModelScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.e(TAG, "❌ 연결된 휴대폰 기기가 없습니다.")
                    return@launch
                }

                // 휴대폰으로 열어야 할 히스토리 ID 페이로드 전송
                val byteArray = historyId.toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, PATH_REQUEST_OPEN_HISTORY, byteArray).await()
                }
                Log.d(TAG, "📱 휴대폰으로 히스토리 오픈 요청 송신 완료: $historyId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 휴대폰 연동 신호 전송 실패", e)
            }
        }
    }

    fun refreshCurrentLocationAndSync(context: Context) {
        val messageClient = Wearable.getMessageClient(context)
        val nodeClient = Wearable.getNodeClient(context)

        // UI 상태를 로딩 중으로 변경하여 프로그레스바 등 시각적 피드백 제공
        updateState { copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.e(TAG, "❌ 연결된 휴대폰 디바이스가 없습니다.")
                    updateState { copy(isLoading = false) }
                    return@launch
                }

                // 휴대폰으로 실시간 위치 전달 신호 송신
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/request_locations", byteArrayOf()).await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 실시간 위치 전달 프로세스 중 에러 발생", e)
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    fun selectFriend(uid: String?) {
        updateState {
            copy(selectedFriendUid = uid)
        }
    }
}