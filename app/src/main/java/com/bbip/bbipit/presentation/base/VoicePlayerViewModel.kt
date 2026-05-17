package com.bbip.bbipit.presentation.base

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.util.AudioPlayer
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신된 음성 메시지의 UI 상태를 관리하는 데이터 클래스입니다.
 */
data class IncomingVoiceUiState(
    val isVisible: Boolean = false,
    val sender: User? = null,
    val currentVoiceMessage: VoiceMessage? = null,
    val currentPosition: Int = 0
)

/**
 * 전역 음성 메시지 수신 및 자동 재생을 관리하는 ViewModel입니다.
 */
@HiltViewModel
class VoicePlayerViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val audioPlayer = AudioPlayer(context)
    private val _uiState = MutableStateFlow(IncomingVoiceUiState())
    val uiState: StateFlow<IncomingVoiceUiState> = _uiState.asStateFlow()

    init {
        startObservingIncomingVoice()
    }

    // 음성 메시지 수신 관찰 및 재생 처리
    private fun startObservingIncomingVoice() {
        viewModelScope.launch {
            authRepository.getAuthStateFlow().collectLatest { uid ->
                if (uid != null) {
                    val subscriptionStartTime = System.currentTimeMillis()
                    voiceRepository.observeIncomingVoice(uid).collect { voiceMessage ->
                        val url = voiceMessage.voiceUrl
                        val createdAt = voiceMessage.createdAt
                        val isNewMessage = createdAt > (subscriptionStartTime - 5000) && !voiceMessage.isRead
                        if (!url.isNullOrEmpty() && isNewMessage) {

                            val senderResult = userRepository.getUserProfile(voiceMessage.senderId)
                            val sender = when (senderResult) {
                                is Result.Success -> senderResult.data
                                is Result.Failure -> null
                            }

                            _uiState.update {
                                it.copy(
                                    isVisible = true,
                                    sender = sender,
                                    currentVoiceMessage = voiceMessage
                                )
                            }

                            audioPlayer.playFromUrl(url) {
                                viewModelScope.launch {
                                    delay(1000)
                                    dismissMessage()
                                }
                            }
                            startPositionTracking()
                            viewModelScope.launch {
                                voiceRepository.markVoiceMessageAsRead(voiceMessage.id)
                            }
                        }
                    }
                } else {
                    audioPlayer.stopAudio()
                    dismissMessage()
                }
            }
        }
    }

    // 수신 메시지 UI 초기화
    fun dismissMessage() {
        _uiState.update { it.copy(isVisible = false, sender = null, currentVoiceMessage = null, currentPosition = 0) }
    }

    // 오디오 재생 위치 추적
    private fun startPositionTracking() {
        viewModelScope.launch {
            var retryCount = 0
            while (isActive && !audioPlayer.isPlaying() && retryCount < 50) {
                delay(200)
                retryCount++
            }
            while (isActive && audioPlayer.isPlaying()) {
                val posSeconds = (audioPlayer.getCurrentPosition() / 1000)
                _uiState.update { it.copy(currentPosition = posSeconds) }
                delay(200)
            }
        }
    }

    // 자원 정리
    override fun onCleared() {
        super.onCleared()
        audioPlayer.stopAudio()
    }
}