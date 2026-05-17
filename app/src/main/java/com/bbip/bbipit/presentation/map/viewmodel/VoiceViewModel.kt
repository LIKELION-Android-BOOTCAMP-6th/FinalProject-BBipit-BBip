package com.bbip.bbipit.presentation.map.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 음성 메시지 무전기 기능의 상태를 관리하는 ViewModel입니다.
 * 녹음 제어 및 음성 파일 업로드 프로세스를 처리합니다.
 */
data class VoiceUiState(
    val isRecording: Boolean = false,
    val selectedTarget: User? = null,
    val recordedFileUri: Uri? = null,
    val uploadedUrl: String? = null,
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val voiceRepository: VoiceRepository
) : BaseViewModel<VoiceUiState>(VoiceUiState()) {

    // 녹음 시작 상태 전환 및 데이터 초기화
    fun startRecording() {
        updateState {
            copy(
                isRecording = true,
                recordedFileUri = null,
                uploadedUrl = null,
                error = null
            )
        }
        Log.d("Voice", "Recording started")
    }

    // 녹음 중지 및 음성 메시지 전송 파이프라인 실행
    fun stopRecording(uri: Uri?, duration: Int) {
        updateState { copy(isRecording = false, recordedFileUri = uri, isUploading = true) }
        Log.d("Voice", "Recording stopped, uri: $uri")

        if (uri == null) {
            updateState { copy(isUploading = false, error = "녹음된 파일이 없습니다.") }
            return
        }

        val senderUid = authRepository.getCurrentUserUid()
        val targetUid = currentState.selectedTarget?.id ?: return

        viewModelScope.launch {
            // 스토리지 업로드 수행
            val uploadResult = voiceRepository.uploadVoiceFile(uri)

            uploadResult.onSuccess { url ->
                Log.d("Voice", "Storage upload success: $url")

                // 업로드 완료된 URL 기반 메시지 전송
                val sendResult = voiceRepository.sendVoiceMessageDirect(senderUid!!, targetUid, url, duration)

                sendResult.onSuccess {
                    updateState { copy(isUploading = false, recordedFileUri = null, uploadedUrl = null) }
                    Log.d("Voice", "Processing complete. Clean code!")
                }.onFailure { e ->
                    updateState { copy(isUploading = false, error = e.message) }
                }

            }.onFailure { e ->
                updateState { copy(isUploading = false, error = "스토리지 업로드 실패: ${e.message}") }
            }
        }
    }

    // 음성 메시지 수신자 설정
    fun setTargetUser(user: User?) {
        updateState { copy(selectedTarget = user) }
    }

    // 에러 상태 초기화
    fun clearError() {
        updateState { copy(error = null) }
    }
}
