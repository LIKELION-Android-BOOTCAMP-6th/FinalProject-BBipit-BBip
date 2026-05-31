package com.bbip.bbipit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val CHANNEL_ID = "watch_central_service_channel"
    private val NOTIFICATION_ID = 888

    companion object {
        private val TAG = "WatchCentralService"
        // 워치 화면 포그라운드 활성화 여부
        var isWatchActiveInForeground: Boolean = false

        // 위치 데이터 공유 스트림
        private val _locationEventBus = MutableSharedFlow<List<WatchLiveStatus>>(replay = 1)
        val locationEventBus = _locationEventBus.asSharedFlow()

        // 휴대폰 서비스 상태 수신을 공유할 이벤트 버스
        private val _mobileStatusEventBus = MutableSharedFlow<String>(replay = 0)
        val mobileStatusEventBus = _mobileStatusEventBus.asSharedFlow()
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
            "/response_locations" -> {
                try {
                    val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                    val type = object : TypeToken<List<WatchLiveStatus>>() {}.type
                    val decryptedList: List<WatchLiveStatus> = Gson().fromJson(jsonStr, type)

                    decryptedList.forEach {
                        Log.d("temp_list", it.toString())
                    }

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

            // 휴대폰으로부터 들어오는 상태 응답패킷 수신 분기 추가
            "/phone_status_reply" -> {
                try {
                    val replyStatus = String(messageEvent.data, Charsets.UTF_8).trim()
                    Log.d(TAG, "📱 [중앙 서비스] 휴대폰 응답 패킷 수신: $replyStatus")

                    // 이벤트 버스를 통해 뷰모델로 전달
                    serviceScope.launch {
                        _mobileStatusEventBus.emit(replyStatus)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [중앙 서비스] 상태 응답 패킷 파싱 실패", e)
                }
            }
        }
    }

    /**
     * 수신 음성 메시지 처리 및 재생
     */
    private fun handleIncomingVoiceMessage(messageEvent: MessageEvent) {
        try {
            val payload = String(messageEvent.data, Charsets.UTF_8)
            val data = Gson().fromJson(payload, Map::class.java)
            val messageId = data["messageId"] as String
            val voiceUrl = data["voiceUrl"] as String
            val senderName = data["senderName"] as String
            val senderProfileImage = data["senderProfileImage"] as String

            val voiceData = WatchVoiceData(messageId, voiceUrl, senderProfileImage, senderName)

            // 💡 중요: 서비스를 블로킹하지 않고, 비동기로 SharedFlow에 데이터만 던진 후 메서드를 종료합니다.
            serviceScope.launch(Dispatchers.Main) {
                VoiceEventBus.emitVoice(voiceData)
            }

            Log.d(TAG, "📥 [중앙 서비스] 무전 패킷 UI 버스로 전달 완료. 서비스 바인딩 해제 허용.")
        } catch (e: Exception) {
            Log.e(TAG, "무전 패킷 처리 중 에러", e)
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