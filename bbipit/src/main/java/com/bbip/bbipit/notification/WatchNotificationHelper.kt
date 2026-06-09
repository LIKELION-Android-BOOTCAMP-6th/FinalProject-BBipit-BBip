package com.bbip.bbipit.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.bbip.bbipit.MainActivity
import com.bbip.bbipit.models.WatchVoiceData

object WatchNotificationHelper {

    private const val CHANNEL_ID = "walkie_choice_channel"

    fun showWalkieNotification(context: Context, voiceData: WatchVoiceData) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID, "무전 수신", NotificationManager.IMPORTANCE_HIGH
        ).apply { enableVibration(true) }
        notificationManager.createNotificationChannel(channel)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("auto_play", true)
            putExtra("message_id", voiceData.messageId)
            putExtra("voice_url", voiceData.voiceUrl)
            putExtra("sender_name", voiceData.senderName)
            putExtra("sender_profile_image", voiceData.senderProfileUrl)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            voiceData.messageId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(voiceData.senderName)
            .setContentText("무전이 왔습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500))
            .setAutoCancel(true)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_play,
                    "재생",
                    tapPendingIntent
                ).build()
            )
            .build()

        notificationManager.notify(voiceData.messageId.hashCode(), notification)
    }
}