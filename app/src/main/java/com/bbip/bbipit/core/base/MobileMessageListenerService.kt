package com.bbip.bbipit.core.base

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.bbip.bbipit.presentation.main.MainActivity
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
            // 자신 및 친구 위치 목록 푸시 요청 대응
            "/request_locations" -> {
                Log.d(TAG, "🔄 워치로부터 사용자 실시간 위치 요청 명령 수신함")
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
                if (hasPermission) {
                    // 1. 권한이 있다면 기존대로 READY 회신
                    messageClient.sendMessage(senderNodeId, "/phone_status_reply", "READY".toByteArray()).await()

                    // 🔥 [핵심 추가] 워치가 켜졌으므로 멈춰있던 스마트폰의 백그라운드 서비스를 강제로 깨웁니다.
                    // 이 액션은 BackgroundListenerService의 onStartCommand를 관통하며 세션을 시작(startSession)시킵니다.
                    triggerBackgroundServiceAction(BackgroundListenerService.ACTION_PUSH_LOCATION_TO_WATCH)
                } else {
                    // 2. 💡 권한이 없다면 NEED_PERMISSION을 보내 워치에 제한 화면을 보여주고
                    messageClient.sendMessage(senderNodeId, "/phone_status_reply", "NEED_PERMISSION".toByteArray()).await()

                    // 3. 💡 백그라운드 상태인 휴대폰 액티비티를 강제로 활성화하여 즉시 권한을 요청하게 만듭니다.
                    val intent = Intent(this@MobileMessageListenerService, MainActivity::class.java).apply {
                        // 백그라운드 서비스에서 액티비티를 실행할 때 필수적인 플래그 설정
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        // 액티비티가 어떤 이유로 켜졌는지 구별할 수 있는 데이터 키-값 전달
                        putExtra("ACTION_REQUEST_PERMISSIONS", true)
                    }
                    startActivity(intent)
                    Log.w(TAG, "⚠️ 블루투스 권한 누락 감지 ➔ 휴대폰 MainActivity 권한 요청 트리거 실행")
                }
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