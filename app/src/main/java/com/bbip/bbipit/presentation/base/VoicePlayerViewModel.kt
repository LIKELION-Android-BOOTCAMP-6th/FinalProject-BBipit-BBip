package com.bbip.bbipit.presentation.base

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.util.AudioPlayer
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
 * 음성 메시지 수신 및 자동 재생을 관리하는 전역 ViewModel
 */
data class IncomingVoiceUiState(
    // 알림 표시 여부
    val isVisible: Boolean = false,
    // 발신자 정보
    val sender: User? = null,
    // 현재 재생 중인 음성 메시지
    val currentVoiceMessage: VoiceMessage? = null,
    // 현재 재생 위치(초 단위)
    val currentPosition: Int = 0
)

/**
 * 전역 음성 재생 상태 및 수신 메시지 처리 클래스
 */
@HiltViewModel
class VoicePlayerViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    // 오디오 재생 관리 객체
    private val audioPlayer = AudioPlayer(context)
    // UI 상태 스트림
    private val _uiState = MutableStateFlow(IncomingVoiceUiState())
    val uiState: StateFlow<IncomingVoiceUiState> = _uiState.asStateFlow()

    init {
        // 음성 수신 감시 시작
        startObservingIncomingVoice()
    }

    /**
     * 인증 상태 변화를 감지하고 새로운 음성 메시지 수신 및 재생 처리
     */
    private fun startObservingIncomingVoice() {
        viewModelScope.launch {
            // 로그인 상태 실시간 감지
            authRepository.getAuthStateFlow().collectLatest { uid ->
                if (uid != null) {
                    // 구독 시작 시점 타임스탬프 기록
                    val subscriptionStartTime = System.currentTimeMillis()
                    voiceRepository.observeIncomingVoice(uid).collect { voiceMessage ->
                        val url = voiceMessage.voiceUrl
                        val createdAt = voiceMessage.createdAt
                        // 신규 메시지 여부 판단 (읽지 않은 메시지)
                        val isNewMessage = createdAt > (subscriptionStartTime - 5000) && !voiceMessage.isRead
                        if (!url.isNullOrEmpty() && isNewMessage) {
                            // 발신자 정보 조회
                            val senderResult = userRepository.getUserProfile(voiceMessage.senderId)
                            val sender = senderResult.getOrNull()
                            // UI 상태 업데이트
                            _uiState.update { 
                                it.copy(
                                    isVisible = true,
                                    sender = sender,
                                    currentVoiceMessage = voiceMessage
                                )
                            }
                            // 오디오 재생
                            audioPlayer.playFromUrl(url) {
                                // 재생 완료 후 1초 대기 및 UI 닫기
                                viewModelScope.launch {
                                    delay(1000)
                                    dismissMessage()
                                }
                            }
                            // 재생 위치 추적 시작
                            startPositionTracking()
                            // 서버 읽음 처리 요청
                            viewModelScope.launch {
                                voiceRepository.markVoiceMessageAsRead(voiceMessage.id)
                            }
                        }
                    }
                } else {
                    // 로그아웃 시 오디오 정지 및 UI 초기화
                    audioPlayer.stopAudio()
                    dismissMessage()
                }
            }
        }
    }

    /**
     * 수신 메시지 UI 숨김 및 상태 초기화
     */
    fun dismissMessage() {
        _uiState.update { it.copy(isVisible = false, sender = null, currentVoiceMessage = null, currentPosition = 0) }
    }

    /**
     * 재생 중인 오디오의 실시간 위치 업데이트
     */
    private fun startPositionTracking() {
        viewModelScope.launch {
            // 오디오 준비 대기
            var retryCount = 0
            while (isActive && !audioPlayer.isPlaying() && retryCount < 50) {
                delay(200)
                retryCount++
            }
            // 재생 위치 업데이트 루프
            while (isActive && audioPlayer.isPlaying()) {
                val posSeconds = (audioPlayer.getCurrentPosition() / 1000)
                _uiState.update { it.copy(currentPosition = posSeconds) }
                delay(200)
            }
        }
    }

    /**
     * ViewModel 해제 시 오디오 자원 정리
     */
    override fun onCleared() {
        super.onCleared()
        audioPlayer.stopAudio()
    }
}