package com.bbip.bbipit.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.bbip.bbipit.models.WatchLiveStatus
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 모바일 기기로부터 수신된 실시간 위치 데이터를 파싱하여 지도 UI 상태를 관리하는 워치용 뷰모델
 */
class WatchMapViewModel(application: Application) : AndroidViewModel(application), MessageClient.OnMessageReceivedListener {

    // 실시간 위치 및 접속 상태 데이터 스트림을 관리하는 은닉화된 가변 상태 플로우
    private val _locationList = MutableStateFlow<List<WatchLiveStatus>>(emptyList())

    // UI 레이어 관찰용 불변성 실시간 위치 목록 상태 플로우
    val locationList: StateFlow<List<WatchLiveStatus>> = _locationList.asStateFlow()

    private val TAG = "WatchMapViewModel"

    /**
     * 뷰모델 초기화 및 Wearable 메시지 수신 리스너 등록
     */
    init {
        Wearable.getMessageClient(application).addListener(this)
    }

    /**
     * Wearable 데이터 레이어 레이아웃을 통해 수신된 메시지 이벤트 처리 콜백 함수
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        // 친구 위치 정보 응답 데이터 경로의 유효성 검증 분기
        if (messageEvent.path == "/response_friends_location") {
            try {
                // 수신된 바이트 배열 데이터의 UTF-8 문자열 변환 및 디코딩 처리
                val jsonStr = String(messageEvent.data, Charsets.UTF_8)

                // 메모리 효율 향상 및 트래픽 절감을 위한 경량화 데이터 모델 역직렬화 타입 정의
                val type = object : TypeToken<List<WatchLiveStatus>>() {}.type
                val decryptedList: List<WatchLiveStatus> = Gson().fromJson(jsonStr, type)

                // 가변 상태 플로우 데이터 갱신을 통한 UI 레이어 실시간 재구성 트리거
                _locationList.value = decryptedList
                Log.d(TAG, "✅ [워치] 경량화 모델로 실시간 위치 동기화 성공! 수신된 인원: ${decryptedList.size}명")
            } catch (e: Exception) {
                Log.e(TAG, "❌ [워치] 위치 패킷 파싱 실패 (클래스 매핑 에러 가능성)", e)
            }
        }
    }

    /**
     * 뷰모델 소멸 및 메모리 해제 시점의 Wearable 리스너 등록 제거 처리
     */
    override fun onCleared() {
        super.onCleared()
        Wearable.getMessageClient(getApplication()).removeListener(this)
    }
}