package com.bbip.bbipit.base

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.util.WatchAudioSender
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 워치 음성 전송 UI 상태 데이터 클래스
 */
data class WatchVoiceUiState(
    val isRecording: Boolean = false, // 녹음 진행 여부
    val isUploading: Boolean = false,  // 파일 업로드 진행 여부
    val selectedTargetUid: String? = null // 수신 대상자 UID
)

/**
 * 워치 음성 무전 송신 뷰모델
 */
class WatchVoiceViewModel(
    private val context: Context
) : WatchBaseViewModel<WatchVoiceUiState>(WatchVoiceUiState()) {

    // 오디오 전송 객체 초기화
    private val audioSender = WatchAudioSender(context)

    // 무전 시작 타임스탬프
    private var startTime = 0L

    // 무전 제한 시간 타이머 코루틴 Job
    private var timerJob: Job? = null
    private val TAG = "WatchVoiceViewModelWatch"

    /**
     * 수신 대상자 UID 설정
     */
    fun setTargetUid(uid: String?) {
        updateState { copy(selectedTargetUid = uid) }
    }

    /**
     * 음성 무전 송신 시작
     */
    fun startVoiceTransmission() {
        // 이미 녹음 중인 경우 예외 처리
        if (currentState.isRecording) return

        // 수신 대상자 유효성 검증
        val targetUid = currentState.selectedTargetUid
        if (targetUid.isNullOrEmpty()) {
            Toast.makeText(context, "무전 상대방 정보가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 오디오 전송 및 타이머 시작
            audioSender.startVoiceTransmission(
                targetUid = targetUid,
                onStartSuccess = {
                    updateState { copy(isRecording = true) }
                    startTime = System.currentTimeMillis()

                    // 기존 타이머 취소 및 4.5초 제한 타이머 시작
                    timerJob?.cancel()
                    timerJob = viewModelScope.launch {
                        delay(4500)
                        if (currentState.isRecording && (System.currentTimeMillis() - startTime >= 4500)) {
                            Log.d(TAG, "⏳ 4.5초 제한 시간 초과로 인한 무전 강제 종료")
                            stopVoiceTransmission()
                        }
                    }
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
    }

    /**
     * 음성 무전 송신 종료 및 업로드 상태 전환
     */
    fun stopVoiceTransmission() {
        // 녹음 상태가 아닌 경우 예외 처리
        if (!currentState.isRecording) return

        // 타이머 종료 및 상태 업데이트
        timerJob?.cancel()
        timerJob = null

        updateState { copy(isRecording = false, isUploading = true) }

        try {
            // 오디오 전송 중지
            audioSender.stopVoiceTransmission()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 전송 완료 대기 후 업로드 상태 종료
        viewModelScope.launch {
            delay(1500)
            updateState { copy(isUploading = false) }
        }
    }

    /**
     * 뷰모델 팩토리 정의 객체
     */
    companion object {
        /**
         * Context 유출 방지를 위한 뷰모델 팩토리 생성
         */
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WatchVoiceViewModel(context.applicationContext) as T
            }
        }
    }
}