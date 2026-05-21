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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 모바일 기기로부터 수신된 Wearable 메시지를 처리하고 오디오 재생을 관리하는 백그라운드 서비스
 */
class WatchVoiceReceiverService : WearableListenerService() {

    // 싱글톤 인스턴스를 통한 워치 오디오 재생 엔진 초기화
    private val audioPlayer by lazy { WatchAudioPlayer.getInstance(this) }

    /**
     * Wearable 데이터 레이어로 연결된 기기에서 메시지 수신 시 호출되는 콜백 함수
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        // 음성 재생 요청 경로 일치 여부 검증
        if (messageEvent.path == "/play_voice") {
            // 수신 데이터의 바이트 배열을 UTF-8 기반 문자열로 디코딩 및 JSON 파싱 처리
            val payload = String(messageEvent.data, Charsets.UTF_8)
            val data = Gson().fromJson(payload, Map::class.java)
            val messageId = data["messageId"] as String
            val voiceUrl = data["voiceUrl"] as String
            val senderName = data["senderName"] as String
            val senderProfileImage = data["senderProfileImage"] as String

            // 메인 스레드 스코프를 활용한 실시간 음성 수신 팝업 UI 표출 이벤트 발행
            CoroutineScope(Dispatchers.Main).launch {
                VoiceEventBus.emitVoice(
                    WatchVoiceData(
                        messageId,
                        voiceUrl,
                        senderProfileImage,
                        senderName))
            }


            // 원격 저장소 URL 기반의 오디오 스트리밍 재생 실행 및 완료 콜백 정의
            audioPlayer.playFromUrl(voiceUrl) {

                // 재생 완료 시점의 메인 스레드 기반 수신 팝업 UI 종료 이벤트 발행
                CoroutineScope(Dispatchers.Main).launch {
                    VoiceEventBus.emitVoice(null)
                }

                // 모바일 기기로의 오디오 컨텐츠 읽음 상태 동기화를 위한 비동기 메시지 전송
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Wearable.getMessageClient(this@WatchVoiceReceiverService)
                            .sendMessage(
                                messageEvent.sourceNodeId, // 송신측 모바일 노드 식별자 지정
                                "/mark_voice_read",
                                messageId.toByteArray(Charsets.UTF_8)
                            ).await()
                        Log.d("WatchVoiceReceiver", "읽음 처리 요청 전송 완료: $messageId")
                    } catch (e: Exception) {
                        Log.e("WatchVoiceReceiver", "읽음 처리 요청 실패", e)
                    }
                }
            }
        }
    }

    /**
     * 서비스 종료 및 메모리 해제 시점의 로그 출력 처리
     */
    override fun onDestroy() {
        Log.d("WatchVoiceReceiver", "Service Destroyed!")
        super.onDestroy()
    }
}