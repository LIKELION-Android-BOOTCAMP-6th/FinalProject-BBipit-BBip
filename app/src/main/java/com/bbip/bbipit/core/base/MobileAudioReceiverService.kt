package com.bbip.bbipit.core.base

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 워치로부터 전송된 오디오 스트림을 수신하여 서버로 전달하는 서비스입니다.
 * Android 14 이상에서 포어그라운드 서비스 정책을 준수합니다.
 */
@AndroidEntryPoint
class MobileAudioReceiverService : Service() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var voiceRepository: VoiceRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "MobileAudioReceiver"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
    }

    /**
     * 워치로부터 음성 파일 저장이 완료되었을 때 호출되는 가상의 오디오 처리 완료 콜백 예시
     * @param localFileUri 업로드할 로컬 오디오 파일의 URI
     * @param receiverId 음성 메시지를 수신할 대상의 UID
     * @param duration 오디오 재생 시간 (단위: 밀리초(ms))
     */
    private fun processAndUploadAudio(localFileUri: Uri, receiverId: String, duration: Int) {
        serviceScope.launch {
            // 사용자 UID 가져오기
            val senderId = authRepository.getCurrentUserUid()
            if (senderId == null) {
                Log.e(TAG, "오디오 전송 실패: 로그인된 사용자가 없습니다.")
                return@launch
            }

            Log.d(TAG, "음성 파일 업로드 시작... Uri: $localFileUri")

            // 음성 파일 업로드
            val uploadResult = voiceRepository.uploadVoiceFile(localFileUri)

            uploadResult.onSuccess { downloadUrl ->
                Log.d(TAG, "스토리지 업로드 성공, DB 전송 시작. URL: $downloadUrl")

                // 메시지 전송
                val sendResult = voiceRepository.sendVoiceMessageDirect(
                    senderId = senderId,
                    receiverId = receiverId,
                    voiceUrl = downloadUrl,
                    duration = duration
                )

                sendResult.onSuccess { isSuccess ->
                    if (isSuccess) {
                        Log.d(TAG, "✅ 워치 무전 음성 메시지 최종 전송 완료 (To: $receiverId)")
                    } else {
                        Log.e(TAG, "❌ 무전 메시지 전송 실패 (서버 반환 false)")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ 무전 메시지 DB 기록 실패: ${error.message}")
                }

            }.onFailure { error ->
                Log.e(TAG, "❌ 오디오 파일 스토리지 업로드 실패: ${error.message}")
            }
        }
    }

    /**
     * 포어그라운드 서비스 알림 표시 및 서비스 시작
     */
    private fun startForegroundServiceNotification() {
        val channelId = "voice_receiver_channel"

        // 알림 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "워치 무전 수신",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // 알림 생성
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("무전기 대기 중")
            .setContentText("워치로부터 음성 신호를 받을 준비가 되었습니다.")
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setOngoing(true)
            .build()

        // 포어그라운드 서비스 시작
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(1, notification)
        }
    }
}