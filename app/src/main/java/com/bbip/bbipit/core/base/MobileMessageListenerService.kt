package com.bbip.bbipit.core.base

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 워치 기기의 무전 연결 상태 확인 요청을 수신하여 처리하는 데이터 레이어 서비스
 */
class MobileMessageListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * 메시지 수신 시 호출되는 이벤트 핸들러
     * 
     * 워치로부터의 상태 확인 요청을 분석하여 현재 폰의 권한 상태를 응답
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("MobileMessageService", "메시지 수신됨. Path: ${messageEvent.path}")

        // 워치 상태 확인 요청 신호 처리
        if (messageEvent.path == "/check_phone_status") {
            val senderNodeId = messageEvent.sourceNodeId

            // 필수 블루투스 권한 확인
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            scope.launch {
                try {
                    val messageClient = Wearable.getMessageClient(this@MobileMessageListenerService)

                    if (hasPermission) {
                        // 권한 확인 완료 시 READY 상태 응답 전송
                        messageClient.sendMessage(
                            senderNodeId,
                            "/phone_status_reply",
                            "READY".toByteArray(Charsets.UTF_8)
                        ).await()
                        Log.d("MobileMessageService", "워치에 READY 응답 전송 완료")
                    } else {
                        // 권한 미확인 시 권한 요청 필요(NEED_PERMISSION) 상태 응답 전송
                        messageClient.sendMessage(
                            senderNodeId,
                            "/phone_status_reply",
                            "NEED_PERMISSION".toByteArray(Charsets.UTF_8)
                        ).await()
                        Log.w("MobileMessageService", "권한 없음: 워치에 NEED_PERMISSION 응답 전송 완료")
                    }
                } catch (e: Exception) {
                    Log.e("MobileMessageService", "워치로 상태 응답 전송 실패", e)
                }
            }
        }
    }
}