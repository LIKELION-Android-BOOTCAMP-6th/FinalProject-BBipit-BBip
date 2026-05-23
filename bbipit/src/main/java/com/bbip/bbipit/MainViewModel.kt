package com.bbip.bbipit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.models.WatchVoiceData
import com.bbip.bbipit.util.VoiceEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 메인 화면 UI 상태 관리 뷰모델
 */
class MainViewModel : ViewModel() {
    // 음성 메시지 수신 상태 플로우
    private val _voiceUiState = MutableStateFlow<WatchVoiceData?>(null)

    // 음성 수신 상태 스트림
    val voiceUiState = _voiceUiState.asStateFlow()

    init {
        // 음성 수신 이벤트 구독 및 상태 업데이트
        viewModelScope.launch {
            VoiceEventBus.incomingVoiceEvent.collect { data ->
                _voiceUiState.value = data
            }
        }
    }

    /**
     * 음성 데이터 상태 초기화
     */
    fun clearVoiceState() {
        _voiceUiState.value = null
    }
}