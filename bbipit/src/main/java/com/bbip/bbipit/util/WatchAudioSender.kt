package com.bbip.bbipit.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.ChannelClient.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 웨어러블 기기에서 스마트폰으로 오디오를 스트리밍하는 기능 담당
 */
class WatchAudioSender(private val context: Context) : MessageClient.OnMessageReceivedListener {

    private val nodeClient = Wearable.getNodeClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val channelClient = Wearable.getChannelClient(context)

    private var isPhoneReady = false
    private var isWaitingForReply = false

    private var audioRecord: AudioRecord? = null
    private var streamingJob: Job? = null

    private var currentChannel: Channel? = null
    private var currentOutputStream: java.io.OutputStream? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    init {
        messageClient.addListener(this)
    }

    /**
     * 리스너 제거 및 자원 정리
     */
    fun destroy() {
        messageClient.removeListener(this)
        stopVoiceTransmission()
    }

    /**
     * 연결된 휴대폰 상태 응답 수신
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/phone_status_reply") {
            val response = String(messageEvent.data, Charsets.UTF_8)
            if (response == "READY") {
                isPhoneReady = true
            } else if (response == "NEED_PERMISSION") {
                isPhoneReady = false
                showToastOnMainThread("⚠️ 휴대폰의 권한 설정을 확인해주세요.")
            }
            isWaitingForReply = false
        }
    }

    /**
     * 오디오 전송 프로세스 시작
     */
    fun startVoiceTransmission(
        onStartSuccess: () -> Unit,
        onStartFailure: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 연결 노드 확인
                val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrNull()
                val phoneNode = nodes?.firstOrNull()

                if (phoneNode == null) {
                    showToastOnMainThread("❌ 연결된 휴대폰이 없습니다.")
                    withContext(Dispatchers.Main) { onStartFailure() }
                    return@launch
                }

                isWaitingForReply = true
                isPhoneReady = false

                // 상태 체크 메시지 전송
                val sendResult = runCatching {
                    messageClient.sendMessage(phoneNode.id, "/check_phone_status", byteArrayOf()).await()
                }

                if (sendResult.isFailure) {
                    showToastOnMainThread("❌ 휴대폰으로 신호를 보낼 수 없습니다.")
                    withContext(Dispatchers.Main) { onStartFailure() }
                    return@launch
                }

                // 폰 응답 대기
                withTimeoutOrNull(1500) {
                    while (isWaitingForReply) { delay(50) }
                }

                if (!isPhoneReady) {
                    if (isWaitingForReply) {
                        showToastOnMainThread("❌ 휴대폰 수신 서비스가 응답하지 않습니다.")
                    }
                    withContext(Dispatchers.Main) { onStartFailure() }
                    return@launch
                }

                Log.d("WatchAudioSender", "휴대폰 통신 확인 완료. 오디오 스트리밍을 시작합니다.")

                // 채널 오픈 및 스트림 바인딩
                val channel = channelClient.openChannel(phoneNode.id, "/audio_stream").await()
                val outputStream = channelClient.getOutputStream(channel).await()

                currentChannel = channel
                currentOutputStream = outputStream

                initAudioRecord()
                audioRecord?.startRecording()

                streamingJob = launch(Dispatchers.IO) {
                    val readBuffer = ByteArray(bufferSize)
                    while (isActive) {
                        try {
                            val readBytes = audioRecord?.read(readBuffer, 0, bufferSize) ?: 0
                            if (readBytes > 0) {
                                currentOutputStream?.write(readBuffer, 0, readBytes)
                            }
                        } catch (e: Exception) {
                            Log.e("WatchAudioSender", "스트리밍 중 에러 발생: ${e.message}")
                            break
                        }
                    }
                }

                withContext(Dispatchers.Main) { onStartSuccess() }

            } catch (e: Exception) {
                Log.e("WatchAudioSender", "오디오 스트리밍 전반적 실패: ${e.message}", e)
                cleanUpResources()
                withContext(Dispatchers.Main) { onStartFailure() }
            }
        }
    }

    /**
     * 오디오 전송 중단 및 자원 정리
     */
    fun stopVoiceTransmission() {
        streamingJob?.cancel()
        streamingJob = null

        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("WatchAudioSender", "AudioRecord 해제 실패", e)
        } finally {
            audioRecord = null
        }

        CoroutineScope(Dispatchers.IO).launch {
            cleanUpResources()
        }
    }

    /**
     * 입출력 채널 및 스트림 자원 정리
     */
    private suspend fun cleanUpResources() {
        try {
            currentOutputStream?.let { stream ->
                runCatching { stream.flush() }
                runCatching { stream.close() }
            }
            currentChannel?.let { channel ->
                runCatching { channelClient.close(channel).await() }
            }
        } catch (e: Exception) {
            Log.e("WatchAudioSender", "자원 정리 중 에러", e)
        } finally {
            currentOutputStream = null
            currentChannel = null
        }
    }

    /**
     * 마이크 녹음 객체 초기화
     */
    @SuppressLint("MissingPermission")
    private fun initAudioRecord() {
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw IllegalStateException("오디오 버퍼 크기를 가져오지 못했습니다.")
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord 초기화에 실패했습니다.")
        }
    }

    /**
     * 메인 스레드에서 토스트 메시지 출력
     */
    private fun showToastOnMainThread(text: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}
