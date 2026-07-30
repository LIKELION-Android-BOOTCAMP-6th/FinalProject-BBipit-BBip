package com.bbip.bbipit.presentation.base

import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.usecase.ListenAndPlayVoiceUseCase
import com.bbip.bbipit.domain.usecase.VoicePlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신 음성 메시지 UI 상태 관리 데이터 클래스
 */
data class IncomingVoiceUiState(
    val isVisible: Boolean = false,
    val senderName: String = "",
    val senderProfileUrl: String = "",
    val currentVoiceMessage: VoiceMessage? = null,
    val currentPosition: Int = 0
)

/**
 * 전역 음성 메시지 수신 및 자동 재생 관리용 ViewModel 클래스
 */
@HiltViewModel
class VoicePlayerViewModel @Inject constructor(
    private val listenAndPlayVoiceUseCase: ListenAndPlayVoiceUseCase,
) : BaseViewModel<IncomingVoiceUiState>(IncomingVoiceUiState()) {

    init {
        observeVoicePlayback()
    }

    /**
     * UseCase의 스트림을 구독하여 오직 UI State만 갱신
     */
    private fun observeVoicePlayback() {
        viewModelScope.launch {
            listenAndPlayVoiceUseCase().collect { state ->
                when (state) {
                    is VoicePlaybackState.Started -> {
                        updateState {
                            copy(
                                isVisible = true,
                                senderName = state.voiceMessage.senderName,
                                senderProfileUrl = state.voiceMessage.senderProfileUrl,
                                currentVoiceMessage = state.voiceMessage,
                                currentPosition = 0
                            )
                        }
                    }
                    is VoicePlaybackState.Playing -> {
                        updateState { copy(currentPosition = state.currentPosition) }
                    }
                    is VoicePlaybackState.Completed, VoicePlaybackState.Error -> {
                        dismissMessage()
                    }
                }
            }
        }
    }

    /**
     * 수신 메시지 UI 및 데이터 초기화 함수
     */
    fun dismissMessage() {
        updateState {
            copy(
                isVisible = false,
                senderName = "",
                senderProfileUrl = "",
                currentVoiceMessage = null,
                currentPosition = 0
            )
        }
    }

    /**
     * 뷰모델 소멸 시 UseCase 내의 플레이어 자원 해제 호출
     */
    override fun onCleared() {
        super.onCleared()
        listenAndPlayVoiceUseCase.release()
    }
}