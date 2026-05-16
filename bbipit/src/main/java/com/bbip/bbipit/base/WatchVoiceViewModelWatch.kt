package com.bbip.bbipit.base

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.util.WatchAudioSender
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 워치 음성 전송 상태 UI 모델
 */
data class WatchVoiceUiState(
    val isRecording: Boolean = false,
    val isUploading: Boolean = false
)

/**
 * 워치 음성 무전기 기능 뷰모델
 */
class WatchVoiceViewModelWatch(
    private val context: Context
) : WatchBaseViewModel<WatchVoiceUiState>(WatchVoiceUiState()) {

    private val audioSender = WatchAudioSender(context)
    private var startTime = 0L

    /**
     * 음성 전송 프로세스 시작
     */
    fun startVoiceTransmission() {
        if (currentState.isRecording) return

        try {
            audioSender.startVoiceTransmission(
                onStartSuccess = {
                    updateState { copy(isRecording = true) }
                    startTime = System.currentTimeMillis()
                },
                onStartFailure = {
                    updateState { copy(isRecording = false) }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            updateState { copy(isRecording = false) }
            Toast.makeText(context, "무전 시작 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }

        viewModelScope.launch {
            delay(4500)
            if (currentState.isRecording && (System.currentTimeMillis() - startTime > 4500)) {
                stopVoiceTransmission()
            }
        }
    }

    /**
     * 음성 전송 프로세스 종료
     */
    fun stopVoiceTransmission() {
        if (!currentState.isRecording) return

        try {
            audioSender.stopVoiceTransmission()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        updateState { copy(isRecording = false, isUploading = true) }

        viewModelScope.launch {
            delay(1500)
            updateState { copy(isUploading = false) }
        }
    }

    /**
     * 뷰모델 생성을 위한 팩토리 제공
     */
    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WatchVoiceViewModelWatch(context.applicationContext) as T
            }
        }
    }
}
