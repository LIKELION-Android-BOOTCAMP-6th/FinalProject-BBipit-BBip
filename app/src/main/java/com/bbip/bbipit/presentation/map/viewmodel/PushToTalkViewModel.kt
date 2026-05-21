package com.bbip.bbipit.presentation.map.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.core.util.AudioRecorder
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 음성 메시지 무전기 기능의 상태를 관리하는 UI State
 */
data class VoiceUiState(
    val isRecording: Boolean = false,       // 현재 녹음 진행 여부
    val selectedTargetUid: String? = null,  // 음성 수신 대상의 UID
    val recordedFileUri: String? = null,    // 녹음 완료된 로컬 파일의 Uri 문자열
    val isUploading: Boolean = false,       // 업로드 및 전송 파이프라인 진행 여부
    val error: String? = null               // 발생한 에러 메시지
)

@HiltViewModel
class PushToTalkViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val voiceRepository: VoiceRepository,
    private val audioRecorder: AudioRecorder
) : BaseViewModel<VoiceUiState>(VoiceUiState()) {

    /**
     * 하드웨어 녹음 시작 및 UI 녹음 상태 전환
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
        Log.d("Voice", "Recording started inside ViewModel")
    }

    /**
     * 하드웨어 녹음 중단 및 음성 메시지 업로드/전송 파이프라인 처리
     * @param duration 녹음 소요 시간 (초 단위)
     */
    fun stopRecording(duration: Int) {
        // 단시간 입력에 대한 하드웨어 버퍼 에러 방지 및 네트워크 리소스 절약을 위한 조기 종료 가드레일
        if (duration < 1) {
            audioRecorder.stop() // 자원 초기화 유도
            updateState {
                copy(
                    isRecording = false,
                    error = "녹음 시간이 너무 짧습니다. 길게 눌러 무전해 주세요."
                )
            }
            Log.d("Voice", "Recording cancelled: Duration too short ($duration s)")
            return
        }

        // 하드웨어 녹음 중단 및 로컬 파일 Uri 획득
        val uri = audioRecorder.stop()

        updateState {
            copy(
                isRecording = false,
                recordedFileUri = uri?.toString(),
                isUploading = true
            )
        }
        Log.d("Voice", "Recording stopped inside ViewModel, uri: $uri")

        // 녹음 파일 유효성 검증 실패 시 예외 처리
        if (uri == null) {
            updateState { copy(isUploading = false, error = "녹음된 파일이 없거나 유효하지 않습니다.") }
            return
        }

        val senderUid = authRepository.getCurrentUserUid()
        val targetUid = currentState.selectedTargetUid

        // 송수신자 인증 정보 유효성 검증 실패 시 예외 처리
        if (senderUid == null || targetUid == null) {
            updateState { copy(isUploading = false, error = "사용자 인증 정보 또는 수신자 정보가 올바르지 않습니다.") }
            return
        }

        viewModelScope.launch {
            // 원격 저장을 위한 Firebase Storage 업로드 비동기 요청
            val uploadResult = voiceRepository.uploadVoiceFile(uri)

            uploadResult.onSuccess { url ->
                Log.d("Voice", "Storage upload success: $url")

                // 상대방 전달을 위한 업로드된 URL 기반의 Firestore 메시지 전송 비동기 요청
                val sendResult = voiceRepository.sendVoiceMessageDirect(senderUid, targetUid, url, duration)

                sendResult.onSuccess {
                    updateState { copy(isUploading = false, recordedFileUri = null) }
                    Log.d("Voice", "Processing complete. Voice message sent successfully.")
                }.onFailure { e ->
                    updateState { copy(isUploading = false, error = e.message) }
                }

            }.onFailure { e ->
                updateState { copy(isUploading = false, error = "스토리지 업로드 실패: ${e.message}") }
            }
        }
    }

    /**
     * 음성 메시지를 수신할 대상 유저의 UID 설정
     */
    fun setTargetUid(uid: String?) {
        updateState { copy(selectedTargetUid = uid) }
    }

    /**
     * UI에 표시된 에러 상태 초기화
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}