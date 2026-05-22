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
 * 워치 음성 전송 프로세스의 실시간 변동을 관리하는 UI 상태 모델
 */
data class WatchVoiceUiState(
    val isRecording: Boolean = false, // 현재 하드웨어 마이크를 통한 오디오 녹음 진행 여부
    val isUploading: Boolean = false,  // 녹음 종료 후 데이터 패킷 업로드 및 통신 파이프라인 구동 여부
    val selectedTargetUid: String? = null // 수신 대상 유저의 UID 상태 필드
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

    // 타이머 코루틴을 제어하기 위한 단일 잡(Job) 변수 선언
    private var timerJob: Job? = null
    private val TAG = "WatchVoiceViewModelWatch"

    /**
     * 🔥 음성 메시지를 수신할 대상 유저의 UID 설정 목적 함수 개설
     */
    fun setTargetUid(uid: String?) {
        updateState { copy(selectedTargetUid = uid) }
    }

    /**
     * 오디오 녹음 및 데이터 스트리밍을 포함하는 음성 무전 전송 프로세스 개시
     */
    fun startVoiceTransmission() {
        // 중복 프로세스 방지를 위한 실시간 녹음 상태 검증 가드레일
        if (currentState.isRecording) return

        // 가드레일: 현재 지정된 타겟 유저의 UID가 올바른지 검증
        val targetUid = currentState.selectedTargetUid
        if (targetUid.isNullOrEmpty()) {
            Toast.makeText(context, "무전 상대방 정보가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 하드웨어 자원 할당 및 스트리밍 채널 활성화 시도
            audioSender.startVoiceTransmission(
                targetUid = targetUid,
                onStartSuccess = {
                    updateState { copy(isRecording = true) }
                    startTime = System.currentTimeMillis()

                    //  이전 타이머가 혹시 남아있다면 취소 후 재할당 (중복 방지)
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
     * 오디오 스트리밍을 중단하고 생성된 패킷의 최종 전송 처리를 수행하는 프로세스 종료 루틴
     */
    fun stopVoiceTransmission() {
        // 비정상적인 종료 요청 차단을 위한 현재 상태 유효성 검증 가드레일
        if (!currentState.isRecording) return

        // 🔥 무전이 정상 또는 강제 종료되었으므로 실행 중인 타이머를 명시적으로 취소합니다.
        timerJob?.cancel()
        timerJob = null

        updateState { copy(isRecording = false, isUploading = true) }

        try {
            // 하드웨어 마이크 자원 해제 및 데이터 스트리밍 채널 차단 요청
            // (내부적으로 500ms 지연 후 스트림이 완전히 닫히도록 앞서 수정함)
            audioSender.stopVoiceTransmission()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 전송 컴포넌트의 비동기 마무리(500ms 지연 및 스마트폰의 인코딩/업로드 시간)를
        // 고려하여 상태 전환 대기 시간을 기존보다 조금 더 여유 있게 조율
        viewModelScope.launch {
            // 워치가 스트림을 완전히 밀어내고(500ms), 폰이 받아 처리하는 시간을 감안해 1.5초~2초 대기
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