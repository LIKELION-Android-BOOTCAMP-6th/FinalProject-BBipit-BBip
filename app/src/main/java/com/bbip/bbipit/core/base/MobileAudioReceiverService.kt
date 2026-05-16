package com.bbip.bbipit.core.base

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.ChannelClient.Channel
import com.google.android.gms.wearable.ChannelClient.ChannelCallback
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * 워치로부터 전송된 오디오 스트림을 수신하여 서버로 전달하는 서비스
 */
@AndroidEntryPoint
class MobileAudioReceiverService : Service() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var voiceRepository: VoiceRepository

    private lateinit var channelClient: ChannelClient
    private val scope = CoroutineScope(Dispatchers.IO)

    // 워치 연결 채널 콜백
    private val channelCallback = object : ChannelCallback() {
        override fun onChannelOpened(channel: Channel) {
            if (channel.path.startsWith("/audio_stream")) {
                receiveWatchAudio(channel)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        channelClient = Wearable.getChannelClient(this)
        channelClient.registerChannelCallback(channelCallback)
        startForegroundServiceNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        channelClient.unregisterChannelCallback(channelCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 워치로부터 PCM 오디오 데이터 수신 및 M4A 인코딩 수행
     */
    private fun receiveWatchAudio(channel: Channel) {
        Log.d("AudioService", "워치 음성 채널 연결 및 데이터 수신 시작")
        scope.launch {
            try {
                // 임시 저장을 위한 PCM 및 결과 M4A 파일 경로 설정
                val pcmFile = File(cacheDir, "watch_temp_recording.pcm")
                val m4aFile = File(cacheDir, "watch_shared_recording.m4a")

                if (pcmFile.exists()) pcmFile.delete()
                if (m4aFile.exists()) m4aFile.delete()

                val inputStream = com.google.android.gms.tasks.Tasks.await(channelClient.getInputStream(channel))
                val startTime = System.currentTimeMillis()

                // 워치로부터 수신된 데이터를 로컬 PCM 파일로 저장
                inputStream.use { input ->
                    FileOutputStream(pcmFile, false).use { outputStream ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.flush()
                    }
                }
                val endTime = System.currentTimeMillis()

                // 데이터 수신 완료 후 채널 연결 종료
                channelClient.close(channel).await()

                // PCM 무결성 검증 및 M4A 인코딩 실행
                if (pcmFile.exists() && pcmFile.length() > 0) {
                    Log.d("AudioService", "PCM 무결성 확인 및 인코딩 시작")

                    encodePcmToM4a(pcmFile, m4aFile)

                    val durationSeconds = ((endTime - startTime) / 1000).toInt().coerceAtLeast(1)

                    if (m4aFile.exists() && m4aFile.length() > 0) {
                        Log.d("AudioService", "M4A 변환 성공 (크기: ${m4aFile.length()} bytes, 시간: ${durationSeconds}초)")
                        sendWatchAudioToServer(Uri.fromFile(m4aFile), durationSeconds)
                    } else {
                        Log.e("AudioService", "M4A 파일 생성 실패")
                    }

                    pcmFile.delete()
                } else {
                    Log.e("AudioService", "수신된 PCM 파일 누락")
                }
            } catch (e: Exception) {
                Log.e("AudioService", "오디오 수신 처리 중 오류 발생", e)
            }
        }
    }

    /**
     * 서버로 오디오 파일 업로드 및 메시지 전송
     */
    private fun sendWatchAudioToServer(fileUri: Uri, duration: Int) {
        scope.launch {
            val senderUid = authRepository.getCurrentUserUid() ?: return@launch
            val targetUid = "Wy102dzyw4buC0V6YJuqxjtf6qA2"

            val uploadResult = voiceRepository.uploadVoiceFile(fileUri)
            uploadResult.onSuccess { url ->
                val sendResult = voiceRepository.sendVoiceMessageDirect(senderUid, targetUid, url, duration)
                sendResult.onSuccess { Log.d("AudioService", "송신 성공") }
                sendResult.onFailure { Log.e("AudioService", "메시지 전송 실패") }
            }
            uploadResult.onFailure { Log.e("AudioService", "파일 업로드 실패") }
        }
    }

    /**
     * 미디어 코덱을 활용한 PCM 파일을 AAC 형식의 M4A 파일로 인코딩
     */
    private fun encodePcmToM4a(pcmFile: File, outputFile: File) {
        val sampleRate = 16000
        val channels = 1
        val bitRate = 64000
        val timeoutUs = 10000L

        // AAC 인코딩 포맷 설정
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
        }

        // 인코더 초기화 및 설정
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        // Muxer를 통한 MP4 컨테이너 생성
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var audioTrackIndex = -1
        var isMuxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val readBuffer = ByteArray(2048)
        val fis = FileInputStream(pcmFile)

        var isPcmEOS = false
        var isCodecEOS = false
        var presentationTimeUs = 0L

        try {
            // 오디오 데이터 처리 루프
            while (!isCodecEOS) {

                // 입력 데이터 코덱 버퍼 전송
                if (!isPcmEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                        inputBuffer.clear()

                        val bytesRead = fis.read(readBuffer)
                        if (bytesRead == -1) {
                            isPcmEOS = true
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            inputBuffer.put(readBuffer, 0, bytesRead)
                            codec.queueInputBuffer(inputBufferIndex, 0, bytesRead, presentationTimeUs, 0)
                            // PCM 데이터 기준 타임스탬프 계산
                            presentationTimeUs += (bytesRead * 1_000_000L) / (sampleRate * channels * 2)
                        }
                    }
                }

                // 처리된 출력 데이터 Muxing
                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex) ?: continue

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size > 0 && isMuxerStarted) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                    }

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isCodecEOS = true
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }

                // 출력 포맷 변경 시 Muxer 트랙 추가 및 시작
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!isMuxerStarted) {
                        audioTrackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        isMuxerStarted = true
                    }
                }
            }
        } finally {
            try { fis.close() } catch (e: Exception) {}

            try {
                codec.stop()
                codec.release()
            } catch (e: Exception) { e.printStackTrace() }

            try {
                if (isMuxerStarted) {
                    muxer.stop()
                }
                muxer.release()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /**
     * 포어그라운드 서비스 알림 표시
     */
    private fun startForegroundServiceNotification() {
        val channelId = "voice_receiver_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "워치 무전 수신", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("무전기 대기 중")
            .setContentText("워치로부터 음성 신호를 받을 준비가 되었습니다.")
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }
}