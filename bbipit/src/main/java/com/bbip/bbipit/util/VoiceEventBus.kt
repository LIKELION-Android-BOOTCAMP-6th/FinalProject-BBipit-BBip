package com.bbip.bbipit.util

import com.bbip.bbipit.models.WatchVoiceData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 워치 전역에서 실시간 음성 수신 이벤트를 전파하고 관찰하기 위한 단일 통로 오버레이 객체
 */
object VoiceEventBus {
    // 음성 데이터 수신 및 종료 상태(null)를 다루는 은닉화된 가변 공유 플로우
    private val _incomingVoiceEvent = MutableSharedFlow<WatchVoiceData?>()

    // UI 컴포저블 및 서비스에서 구독 가능한 불변성 실시간 음성 이벤트 스트림
    val incomingVoiceEvent = _incomingVoiceEvent.asSharedFlow()

    /**
     * 수신된 오디오 패킷 정보 또는 종료 신호를 스트림에 주입하여 구독자들에게 전파
     */
    suspend fun emitVoice(data: WatchVoiceData?) {
        _incomingVoiceEvent.emit(data)
    }
}