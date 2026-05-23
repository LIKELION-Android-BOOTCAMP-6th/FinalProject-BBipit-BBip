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
    private val authRepository: AuthRepository,
    private val voiceRepository: VoiceRepository,
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
     * 무전 녹음 중단 및 파일 전송 함수
     */
    fun stopRecording(duration: Int) {
        // 최소 녹음 시간 제한 가드레일
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

        // 전송 딜레이 보정을 위해 오디오 길이에 1초 추가
        val correctedDuration = duration + 1

        viewModelScope.launch {
            // 파일 기록 안정화를 위해 대기
            kotlinx.coroutines.delay(500)

            // 녹음 중단 및 파일 Uri 획득
            val uri = audioRecorder.stop()

            updateState { copy(recordedFileUri = uri?.toString()) }
            Log.d(TAG, "Recording stopped inside ViewModel, uri: $uri")

            // 파일 유효성 검증
            if (uri == null) {
                updateState { copy(isUploading = false, error = "녹음된 파일이 없거나 유효하지 않습니다.") }
                return@launch
            }

            val senderUid = authRepository.getCurrentUserUid()
            val targetUid = currentState.selectedTargetUid

            // 인증 및 수신자 정보 확인
            if (senderUid == null || targetUid == null) {
                updateState { copy(isUploading = false, error = "사용자 인증 정보 또는 수신자 정보가 올바르지 않습니다.") }
                return@launch
            }

            // 스토리지 파일 업로드
            val uploadResult = voiceRepository.uploadVoiceFile(uri)

            uploadResult.onSuccess { url ->
                Log.d("Voice", "Storage upload success: $url")

                // 상대방에게 무전 메시지 전송
                val sendResult =
                    voiceRepository.sendVoiceMessageDirect(senderUid, targetUid, url, correctedDuration)

                sendResult.onSuccess {
                    updateState { copy(isUploading = false, recordedFileUri = null) }
                }.onFailure { e ->
                    updateState { copy(isUploading = false, error = e.message) }
                }
            }.onFailure { e ->
                updateState { copy(isUploading = false, error = "스토리지 업로드 실패: ${e.message}") }
            }
        }
    }

    /**
     * 수신자 UID 설정 함수
     */
    fun setTargetUid(uid: String?) {
        updateState { copy(selectedTargetUid = uid) }
    }

    /**
     * 에러 상태 초기화 함수
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}