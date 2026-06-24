package com.bbip.bbipit.presentation.map.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.core.util.AudioRecorder
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import com.bbip.bbipit.domain.usecase.SendVoiceMessageUseCase
import com.bbip.bbipit.domain.usecase.VoiceUploadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 음성 메시지 무전기 관련 UI 상태 데이터 클래스
 */
data class VoiceUiState(
    val isRecording: Boolean = false,       // 녹음 진행 여부
    val selectedTargetUid: String? = null,  // 수신자 UID
    val recordedFileUri: String? = null,    // 로컬 파일 Uri
    val isUploading: Boolean = false,       // 업로드 및 전송 여부
    val error: String? = null               // 에러 메시지
)

/**
 * 음성 메시지 무전기 관련 ViewModel
 */
@HiltViewModel
class PushToTalkViewModel @Inject constructor(
    private val sendVoiceMessageUseCase: SendVoiceMessageUseCase, // 유즈케이스로 교체
    private val audioRecorder: AudioRecorder
) : BaseViewModel<VoiceUiState>(VoiceUiState()) {

    val TAG = "PushToTalkViewModel"

    /**
     * 무전 녹음 시작 함수
     */
    fun startRecording() {
        updateState {
            copy(
                isRecording = true,
                recordedFileUri = null,
                error = null
            )
        }
        audioRecorder.start()
        Log.d(TAG, "Recording started inside ViewModel")
    }

    /**
     * 무전 녹음 중단 및 파일 전송 함수 (UseCase 위임으로 간소화)
     */
    fun stopRecording(duration: Int) {
        // UI 검증 규칙: 최소 녹음 시간 제한 가드레일
        if (duration < 1) {
            audioRecorder.stop()
            updateState {
                copy(
                    isRecording = false,
                    error = "녹음 시간이 너무 짧습니다. 길게 눌러 무전해 주세요."
                )
            }
            Log.d(TAG, "Recording cancelled: Duration too short ($duration s)")
            return
        }

        updateState { copy(isRecording = false, isUploading = true) }

        viewModelScope.launch {
            // 녹음 정지 및 파일 로컬 Uri 획득
            val uri = audioRecorder.stop()
            updateState { copy(recordedFileUri = uri?.toString()) }
            Log.d(TAG, "Recording stopped inside ViewModel, uri: $uri")

            // 복잡한 전송 프로세스는 UseCase에 전적으로 위임
            val result = sendVoiceMessageUseCase(
                audioUri = uri,
                duration = duration,
                targetUid = currentState.selectedTargetUid
            )

            // 결과 상태에 맞춰 UI State만 단순 업데이트
            when (result) {
                is VoiceUploadResult.Success -> {
                    updateState { copy(isUploading = false, recordedFileUri = null) }
                }
                is VoiceUploadResult.Failure -> {
                    updateState { copy(isUploading = false, error = result.message) }
                }
                is VoiceUploadResult.FileReady -> {
                    updateState { copy(recordedFileUri = result.fileUri) }
                }
            }
        }
    }

    fun setTargetUid(uid: String?) {
        updateState { copy(selectedTargetUid = uid) }
    }

    fun clearError() {
        updateState { copy(error = null) }
    }
}