package com.bbip.bbipit.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WalkieChoiceActionReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "WalkieChoiceReceiver"
        const val ACTION_LISTEN_ON_WATCH = "com.bbip.bbipit.ACTION_LISTEN_ON_WATCH"
        const val ACTION_LISTEN_ON_PHONE = "com.bbip.bbipit.ACTION_LISTEN_ON_PHONE"
        const val EXTRA_VOICE_ID = "extra_voice_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val voiceId = intent.getStringExtra(EXTRA_VOICE_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                val path = when (intent.action) {
                    ACTION_LISTEN_ON_WATCH -> "/listen_on_watch"
                    ACTION_LISTEN_ON_PHONE -> "/listen_on_phone"
                    else -> return@launch
                }

                val nodeClient = Wearable.getNodeClient(context)
                val messageClient = Wearable.getMessageClient(context)
                val nodes = nodeClient.connectedNodes.await()

                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, voiceId.toByteArray(Charsets.UTF_8)).await()
                    Log.d(TAG, "✅ $path 전송 완료: ${node.id}")
                }

                // 알림 닫기
                if (notificationId != -1) {
                    val nm = context.getSystemService(NotificationManager::class.java)
                    nm.cancel(notificationId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 전송 실패: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}