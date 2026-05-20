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
 * 워치 음성 전송 프로세스의 실시간 변동을 관리하는 UI 상태 모델
 */
data class WatchVoiceUiState(
    val isRecording: Boolean = false, // 현재 하드웨어 마이크를 통한 오디오 녹음 진행 여부
    val isUploading: Boolean = false  // 녹음 종료 후 데이터 패킷 업로드 및 통신 파이프라인 구동 여부
)

/**
 * 워치 단독 음성 무전 송신 기능의 생명주기 및 이벤트를 제어하는 뷰모델
 */
class WatchVoiceViewModelWatch(
    private val context: Context
) : WatchBaseViewModel<WatchVoiceUiState>(WatchVoiceUiState()) {

    // 웨어러블 데이터 계층 통신 및 오디오 인코딩 스트리밍 처리를 위한 전송 객체 초기화
    private val audioSender = WatchAudioSender(context)

    // 무전 제한 시간 초과 여부 검증을 위한 타임스탬프 기록 변수
    private var startTime = 0L

    /**
     * 오디오 녹음 및 데이터 스트리밍을 포함하는 음성 무전 전송 프로세스 개시
     */
    fun startVoiceTransmission() {
        // 중복 프로세스 방지를 위한 실시간 녹음 상태 검증 가드레일
        if (currentState.isRecording) return

        try {
            // 하드웨어 자원 할당 및 스트리밍 채널 활성화 시도
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

        // 과도한 네트워크 자원 소모 및 기기 과열 방지를 위한 최대 무전 제한 시간(4.5초) 타이머 루틴
        viewModelScope.launch {
            delay(4500)
            // 임계 시간 초과 시점까지 사용자가 버튼을 누르고 있는 경우 강제 종료 파이프라인 실행
            if (currentState.isRecording && (System.currentTimeMillis() - startTime > 4500)) {
                stopVoiceTransmission()
            }
        }
    }

    /**
     * 오디오 스트리밍을 중단하고 생성된 패킷의 최종 전송 처리를 수행하는 프로세스 종료 루틴
     */
    fun stopVoiceTransmission() {
        // 비정상적인 종료 요청 차단을 위한 현재 상태 유효성 검증 가드레일
        if (!currentState.isRecording) return

        try {
            // 하드웨어 마이크 자원 해제 및 데이터 스트리밍 채널 차단 요청
            audioSender.stopVoiceTransmission()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // UI 상태를 파일 전송중으로 전환하여 사용자 피드백 제공
        updateState { copy(isRecording = false, isUploading = true) }

        // 전송 컴포넌트의 비동기 마무리를 고려한 임시 지연 및 전송 완료 상태 전환 처리
        viewModelScope.launch {
            delay(1500)
            updateState { copy(isUploading = false) }
        }
    }

    /**
     * Context 의존성 주입 및 액티비티 스코프 격리를 방지하기 위한 뷰모델 팩토리 객체 정의
     */
    companion object {
        /**
         * 메모리 누수 방지를 위한 ApplicationContext 참조 기반의 뷰모델 생성 팩토리 반환
         */
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WatchVoiceViewModelWatch(context.applicationContext) as T
            }
        }
    }
}