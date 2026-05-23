package com.bbip.bbipit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bbip.bbipit.models.WatchLiveStatus
import com.bbip.bbipit.models.WatchVoiceData
import com.bbip.bbipit.util.VoiceEventBus
import com.bbip.bbipit.util.WatchAudioPlayer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await

/**
 * 워치 데이터 통신 및 음성 재생 중앙 서비스
 */
class WatchCentralService : WearableListenerService() {

    private val audioPlayer by lazy { WatchAudioPlayer.getInstance(this) }
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val TAG = "WatchCentralService"
    private val CHANNEL_ID = "watch_central_service_channel"
    private val NOTIFICATION_ID = 888

    companion object {
        // 워치 화면 포그라운드 활성화 여부
        var isWatchActiveInForeground: Boolean = false

        // 위치 데이터 공유 스트림
        private val _locationEventBus = MutableSharedFlow<List<WatchLiveStatus>>(replay = 1)
        val locationEventBus = _locationEventBus.asSharedFlow()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * 수신 메시지 분기 처리
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "📥 [중앙 서비스] 패킷 수신. Path: ${messageEvent.path}")

        when (messageEvent.path) {
            // 워치 상태 요청 처리
            "/request_watch_status" -> {
                serviceScope.launch {
                    sendWatchStateToPhone(isWatchActiveInForeground)
                }
            }

            // 친구 위치 목록 업데이트 처리
            "/response_friends_location" -> {
                try {
                    val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                    val type = object : TypeToken<List<WatchLiveStatus>>() {}.type
                    val decryptedList: List<WatchLiveStatus> = Gson().fromJson(jsonStr, type)

                    serviceScope.launch {
                        _locationEventBus.emit(decryptedList)
                    }
                    Log.d(TAG, "✅ [중앙 서비스] 위치 동기화 성공! 인원: ${decryptedList.size}명")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [중앙 서비스] 위치 패킷 파싱 실패", e)
                }
            }

            // 음성 스트리밍 재생 처리
            "/play_voice" -> {
                handleIncomingVoiceMessage(messageEvent)
            }
        }
    }

    /**
     * 수신 음성 메시지 처리 및 재생
     */
    private fun handleIncomingVoiceMessage(messageEvent: MessageEvent) {
        val executionDeferred = CompletableDeferred<Unit>()

        try {
            // 포어그라운드 서비스 시작
            startForeground(NOTIFICATION_ID, createVoiceNotification())

            val payload = String(messageEvent.data, Charsets.UTF_8)
            val data = Gson().fromJson(payload, Map::class.java)
            val messageId = data["messageId"] as String
            val voiceUrl = data["voiceUrl"] as String
            val senderName = data["senderName"] as String
            val senderProfileImage = data["senderProfileImage"] as String

            // UI 팝업 표시 이벤트 발송
            serviceScope.launch(Dispatchers.Main) {
                VoiceEventBus.emitVoice(
                    WatchVoiceData(messageId, voiceUrl, senderProfileImage, senderName)
                )
            }

            // 오디오 파일 재생 및 완료 콜백 처리
            audioPlayer.playFromUrl(voiceUrl) {
                Log.d(TAG, "🎵 오디오 재생 완료 콜백 진입: $messageId")

                // UI 팝업 닫기 트리거 발송
                serviceScope.launch(Dispatchers.Main) {
                    VoiceEventBus.emitVoice(null)
                }

                // 모바일 기기로 읽음 상태 전송
                serviceScope.launch {
                    try {
                        val nodeClient = Wearable.getNodeClient(this@WatchCentralService)
                        val messageClient = Wearable.getMessageClient(this@WatchCentralService)
                        val nodes = nodeClient.connectedNodes.await()
                        val phoneNode = nodes.firstOrNull()

                        if (phoneNode != null) {
                            messageClient.sendMessage(
                                phoneNode.id,
                                "/mark_voice_read",
                                messageId.toByteArray(Charsets.UTF_8)
                            ).await()
                            Log.d(TAG, "✅ 스마트폰으로 읽음 신호 전송 성공: $messageId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 읽음 처리 요청 전송 중 에러", e)
                    } finally {
                        executionDeferred.complete(Unit)
                    }
                }
            }

            // 재생 완료 시점까지 코루틴 블로킹 유지
            runBlocking {
                executionDeferred.await()
            }

        } catch (e: Exception) {
            Log.e(TAG, "무전 패킷 처리 중 치명적 에러 발생", e)
        } finally {
            stopForeground(true)
        }
    }

    /**
     * 모바일 기기로 워치 화면 활성화 상태 전송
     */
    private suspend fun sendWatchStateToPhone(isActive: Boolean) {
        try {
            val messageClient = Wearable.getMessageClient(this)
            val path = "/watch_state"
            val payload = isActive.toString().toByteArray()
            val nodes = Wearable.getNodeClient(this).connectedNodes.await()

            for (node in nodes) {
                messageClient.sendMessage(node.id, path, payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "상태 전송 중 실패", e)
        }
    }

    /**
     * 음성 재생 포어그라운드 알림 생성
     */
    private fun createVoiceNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("무전 수신 중")
            .setContentText("음성 메시지를 재생하고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * 알림 채널 생성
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "워치 중앙 관리 알림",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Central Service Destroyed!")
        serviceJob.cancel()
        super.onDestroy()
    }
}