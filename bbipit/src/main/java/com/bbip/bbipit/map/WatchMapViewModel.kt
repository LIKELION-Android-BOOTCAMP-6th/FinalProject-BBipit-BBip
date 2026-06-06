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

/**
 * 지도 UI 상태 데이터 클래스
 */
data class WatchMapUiState(
    val liveStatusList: List<WatchLiveStatus> = emptyList(),
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
    }

    fun refreshCurrentLocationAndSync(context: Context) {
        val messageClient = Wearable.getMessageClient(context)
        val nodeClient = Wearable.getNodeClient(context)
        viewModelScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.e(TAG, "❌ 연결된 휴대폰 디바이스가 없습니다.")
                    return@launch
                }

                // 휴대폰으로 실시간 위치 전달 신호 송신
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/request_locations", byteArrayOf()).await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 실시간 위치 전달 프로세스 중 에러 발생", e)
            }
        }
    }

    fun selectFriend(uid: String?) {
        updateState {
            copy(selectedFriendUid = uid)
        }
    }
}