package com.bbip.bbipit.core.base

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

/**
 * 워치 송신 메시지 상시 리스닝 및 서비스 트리거용 리스너 서비스
 */
class MobileMessageListenerService : WearableListenerService() {

    // 로그 출력용 클래스 식별 태그
    private val TAG = "MobileMessageListenerService"

    // 권한 체크 응답 전송용 작업 관리자
    private val serviceJob = SupervisorJob()

    // 백그라운드 연산 처리용 비동기 스코프
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "📩 워치 신호 수신됨: Path = ${messageEvent.path}")

        // 워치 메시지 경로별 작업 분기
        when (messageEvent.path) {
            // 친구 위치 목록 푸시 요청 대응
            "/request_friends_location" -> {
                triggerBackgroundServiceAction(BackgroundListenerService.ACTION_PUSH_LOCATION_TO_WATCH)
            }
            // 음성 메시지 읽음 처리 대응
            "/mark_voice_read" -> {
                // 음성 메시지 식별자 추출
                val voiceMessageId = String(messageEvent.data, Charsets.UTF_8).trim()
                triggerBackgroundServiceAction(
                    action = BackgroundListenerService.ACTION_UPDATE_VOICE_READ,
                    extraKey = BackgroundListenerService.EXTRA_VOICE_MESSAGE_ID,
                    extraValue = voiceMessageId
                )
            }
            // 기기 연동 및 권한 체크 대응
            "/check_phone_status" -> {
                handleMobileStatusCheck(messageEvent.sourceNodeId)
            }
        }
    }

    /**
     * 서비스 대상 명령 위임 및 실행 함수
     */
    private fun triggerBackgroundServiceAction(action: String, extraKey: String? = null, extraValue: String? = null) {
        // 백그라운드 리스너 서비스 인텐트 생성
        val intent = Intent(this, BackgroundListenerService::class.java).apply {
            this.action = action
            if (extraKey != null && extraValue != null) {
                putExtra(extraKey, extraValue)
            }
        }

        try {
            // 안드로이드 버전별 서비스 실행
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            // 서비스 가동 실패 예외 처리
            Log.e(TAG, "포어그라운드 서비스 명령 위임 실패: ${e.message}")
        }
    }

    /**
     * 필수 권한 검증 및 워치 대상 상태 회신 함수
     */
    private fun handleMobileStatusCheck(senderNodeId: String) {
        // 블루투스 권한 검증
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        scope.launch {
            runCatching {
                // 권한 확인 결과 구성 및 워치로 메시지 송신
                val messageClient = Wearable.getMessageClient(this@MobileMessageListenerService)
                val status = if (hasPermission) "READY" else "NEED_PERMISSION"

                messageClient.sendMessage(senderNodeId, "/phone_status_reply", status.toByteArray()).await()
            }.onFailure { e ->
                // 메시지 전송 실패 예외 처리
                Log.e(TAG, "❌ [WearableService] 권한 체크 응답 실패", e)
            }
        }
    }

    /**
     * 서비스 종료 콜백 함수
     */
    override fun onDestroy() {
        super.onDestroy()
        // 진행 중인 모든 작업 취소
        serviceJob.cancel()
    }
}