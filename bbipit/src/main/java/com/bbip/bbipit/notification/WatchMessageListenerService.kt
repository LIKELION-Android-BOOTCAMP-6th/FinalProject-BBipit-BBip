package com.bbip.bbipit.notification

import android.annotation.SuppressLint
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bbip.bbipit.MainActivity
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson

class WatchMessageListenerService : WearableListenerService() {

    companion object {
        const val TAG = "WatchMessageListener"
        const val CHANNEL_ID = "walkie_choice_channel"
    }

    @SuppressLint("WearRecents")
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/launch_and_play" -> {
                val data = Gson().fromJson(String(messageEvent.data), Map::class.java)
                val messageId = data["messageId"] as? String ?: return
                val voiceUrl = data["voiceUrl"] as? String ?: return
                val senderName = data["senderName"] as? String ?: "알 수 없음"
                val senderProfileImage = data["senderProfileImage"] as? String ?: ""

                Log.d(TAG, "⌚ /launch_and_play 수신 - 워치 앱 실행 + 즉시 재생 트리거")

                val launchIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("auto_play", true)
                    putExtra("message_id", messageId)
                    putExtra("voice_url", voiceUrl)
                    putExtra("sender_name", senderName)
                    putExtra("sender_profile_image", senderProfileImage)
                }
                startActivity(launchIntent)
                Log.d(TAG, "✅ 워치 MainActivity 실행 완료 - messageId: $messageId")
            }
        }
    }

    private fun showWalkieChoiceNotification(
        notificationId: String,
        audioId: String,
        senderName: String,
        voiceUrl: String
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "무전 수신", NotificationManager.IMPORTANCE_HIGH
            ).apply { enableVibration(true) }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("auto_play", true)
            putExtra("message_id", audioId)
            putExtra("voice_url", voiceUrl)
            putExtra("sender_name", senderName)
            putExtra("sender_profile_image", "")
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            notificationId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(senderName)
            .setContentText("무전이 왔습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500))
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        notificationManager.notify(notificationId.hashCode(), notification)
    }
}