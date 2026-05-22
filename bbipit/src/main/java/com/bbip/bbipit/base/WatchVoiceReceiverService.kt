package com.bbip.bbipit.base

import android.util.Log
import com.bbip.bbipit.models.WatchVoiceData
import com.bbip.bbipit.util.VoiceEventBus
import com.bbip.bbipit.util.WatchAudioPlayer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 모바일 기기로부터 수신된 Wearable 메시지를 처리하고 오디오 재생을 관리하는 백그라운드 서비스
 */
class WatchVoiceReceiverService : WearableListenerService() {

    // 싱글톤 인스턴스를 통한 워치 오디오 재생 엔진 초기화
    private val audioPlayer by lazy { WatchAudioPlayer.getInstance(this) }

    // 🔥 서비스 고유의 독립된 안전 코루틴 스코프 정의
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    /**
     * Wearable 데이터 레이어로 연결된 기기에서 메시지 수신 시 호출되는 콜백 함수
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        // 음성 재생 요청 경로 일치 여부 검증
        if (messageEvent.path == "/play_voice") {
            try {
                val payload = String(messageEvent.data, Charsets.UTF_8)
                val data = Gson().fromJson(payload, Map::class.java)
                val messageId = data["messageId"] as String
                val voiceUrl = data["voiceUrl"] as String
                val senderName = data["senderName"] as String
                val senderProfileImage = data["senderProfileImage"] as String

                // 전역 이벤트 버스를 통해 UI 오버레이 팝업 트리거
                serviceScope.launch(Dispatchers.Main) {
                    VoiceEventBus.emitVoice(
                        WatchVoiceData(messageId, voiceUrl, senderProfileImage, senderName)
                    )
                }

                // OS의 서비스 라이프사이클 강제 종료에 영향을 받지 않는 고유 핸들러 스레드 기반 재생 실행
                audioPlayer.playFromUrl(voiceUrl) {
                    // [오디오 재생 완료 콜백 시점]
                    serviceScope.launch(Dispatchers.Main) {
                        VoiceEventBus.emitVoice(null) // 팝업 닫기
                    }

                    // 카운터 기반의 stopSelf() 대신 독립 스코프에서 네트워크 동기화 직접 처리
                    serviceScope.launch {
                        try {
                            Wearable.getMessageClient(this@WatchVoiceReceiverService)
                                .sendMessage(
                                    messageEvent.sourceNodeId,
                                    "/mark_voice_read",
                                    messageId.toByteArray(Charsets.UTF_8)
                                ).await()
                            Log.d("WatchVoiceReceiver", "✅ 읽음 처리 요청 전송 완료: $messageId")
                        } catch (e: Exception) {
                            Log.e("WatchVoiceReceiver", "❌ 읽음 처리 요청 실패", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WatchVoiceReceiver", "패킷 처리 중 치명적 에러 발생", e)
            }
        }
    }

    /**
     * 서비스 종료 및 메모리 해제 시점의 로그 출력 처리
     */
    override fun onDestroy() {
        Log.d("WatchVoiceReceiver", "Service Destroyed!")
        serviceScope.cancel() // 서비스 해제 시 비동기 코루틴 안전 통합 취소
        super.onDestroy()
    }
}