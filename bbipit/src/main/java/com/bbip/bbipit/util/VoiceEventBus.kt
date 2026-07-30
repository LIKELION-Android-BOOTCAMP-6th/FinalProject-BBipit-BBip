package com.bbip.bbipit.util

import com.bbip.bbipit.models.WatchVoiceData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 실시간 음성 수신 이벤트 전파 객체
 */
object VoiceEventBus {
    // 음성 데이터 수신 공유 플로우
    private val _incomingVoiceEvent = MutableSharedFlow<WatchVoiceData?>()

    // 음성 수신 이벤트 스트림
    val incomingVoiceEvent = _incomingVoiceEvent.asSharedFlow()

    /**
     * 음성 데이터 또는 종료 신호 발생
     */
    suspend fun emitVoice(data: WatchVoiceData?) {
        _incomingVoiceEvent.emit(data)
    }
}