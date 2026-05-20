package com.bbip.bbipit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.models.WatchVoiceData
import com.bbip.bbipit.util.VoiceEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 전역 이벤트 버스로부터 음성 수신 이벤트를 전달받아 메인 UI 팝업 상태를 제어하는 뷰모델
 */
class MainViewModel : ViewModel() {
    // 음성 메시지 수신 팝업의 활성화 데이터를 관리하는 은닉화된 가변 상태 플로우
    private val _voiceUiState = MutableStateFlow<WatchVoiceData?>(null)

    // UI 레이어 관찰용 불변성 수신 음성 상태 플로우
    val voiceUiState = _voiceUiState.asStateFlow()

    /**
     * 뷰모델 초기화 및 전역 백그라운드 서비스발 오디오 수신 공유 스트림 구독 개시
     */
    init {
        viewModelScope.launch {
            // 이벤트 버스의 실시간 오디오 패킷 발생 감지 및 UI 상태 동기화
            VoiceEventBus.incomingVoiceEvent.collect { data ->
                _voiceUiState.value = data
            }
        }
    }

    /**
     * UI 수신 팝업 종료 요청 시 오디오 데이터 상태값 초기화
     */
    fun clearVoiceState() {
        _voiceUiState.value = null
    }
}