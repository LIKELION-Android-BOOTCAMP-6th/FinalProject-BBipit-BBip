package com.bbip.bbipit.core.base

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.FriendRepository
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import com.bbip.bbipit.presentation.main.MainActivity
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * 백그라운드 데이터 동기화 및 워치 통신 관리 서비스
 * 앱의 전역 스코프에서 실시간 위치 추적, 오디오 스트리밍 인코딩, 워치 상태 동기화 및 시스템 알림 발행 수행
 */
@AndroidEntryPoint
class BackgroundListenerService : Service() {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var voiceRepository: VoiceRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var liveStatusRepository: LiveStatusRepository

    @Inject
    lateinit var friendRepository: FriendRepository

    @Inject
    lateinit var lifeCycleManager: LifeCycleManager

    @Inject
    lateinit var notificationRepository: NotificationRepository

    // 백그라운드 작업 관리용 코루틴 식별자
    private val serviceJob = SupervisorJob()

    // 백그라운드 연산 처리용 비동기 스코프
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)

    // 로그 출력용 클래스 식별 태그
    private val TAG = "BackgroundListenerService"

    // 음성 메시지 구독 관리용 작업 단위
    private var voiceObservationJob: Job? = null

    // 워치 연결 상태 관리 매니저
    private lateinit var watchConnectionManager: WatchConnectionManager

    // 실시간 위치 추적용 클라이언트
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 주기적 위치 정보 수신 리스너
    private lateinit var locationCallback: LocationCallback

    // 워치 오디오 스트림 통신 인터페이스
    private lateinit var channelClient: ChannelClient

    // 워치 제어 메시지 송수신 클라이언트
    private val messageClient by lazy { Wearable.getMessageClient(this) }

    // 워치 노드 연결 상태 관리 컴포넌트
    private val nodeClient by lazy { Wearable.getNodeClient(this) }

    // 워치 앱의 화면 활성화 여부 플래그
//    private var isWatchInForeground = false

    // 알림 최초 로딩 스킵용 플래그
    private var isInitialData = true

    // 과거 알림 필터링용 서비스 시작 타임스탬프
    private val serviceStartTime = System.currentTimeMillis()

    // 배너 표출이 완료된 알림 식별자 저장소
    private val notifiedIds = mutableSetOf<String>()

    /**
     * 웨어러블 디바이스 및 시스템 채널 식별자 통합 상수 공간
     */
    companion object {

        // 워치 상태 확인 요청 경로
        const val PATH_REQUEST_WATCH_STATUS = "/request_watch_status"

        // 친구 위치 목록 워치 전송 경로
        const val PATH_RESPONSE_FRIENDS_LOCATION = "/response_locations"

        // 워치 오디오 출력 명령 경로
        const val PATH_PLAY_VOICE = "/play_voice"

        // 오디오 스트리밍 채널 접두사
        const val PATH_AUDIO_STREAM_PREFIX = "/audio_stream"

        // 위치 정보 워치 강제 푸시 액션 명칭
        const val ACTION_PUSH_LOCATION_TO_WATCH = "PUSH_LOCATION"

        // 음성 메시지 읽음 확인 갱신 액션 명칭
        const val ACTION_UPDATE_VOICE_READ = "UPDATE_VOICE_READ"

        // 읽음 상태 갱신 대상 데이터 식별자 키값
        const val EXTRA_VOICE_MESSAGE_ID = "extra_voice_message_id"

        // 무전 대기용 상주 알림 채널 식별자
        const val CHANNEL_ID_VOICE = "voice_receiver_channel"

        // 푸시 배너 표출용 알림 채널 식별자
        const val CHANNEL_ID_ALERT = "phone_alert_channel"
    }

    /**
     * 오디오 스트림 채널 감지 콜백
     */
    private val channelCallback = object : ChannelClient.ChannelCallback() {
        override fun onChannelOpened(channel: ChannelClient.Channel) {
            if (channel.path.startsWith(PATH_AUDIO_STREAM_PREFIX)) {
                receiveWatchAudio(channel)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 서비스 초기 생성 및 데이터 옵저버 가동 함수
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BackgroundListenerService onCreate 호출됨")

        watchConnectionManager = WatchConnectionManager(this).apply { startMonitoring() }

        // 워치 통신 리스너 등록
        channelClient = Wearable.getChannelClient(this).apply {
            registerChannelCallback(channelCallback)
        }

        scope.launch {
            combine(
                lifeCycleManager.isAppInForeground,             // 폰 화면 활성화 상태
                watchConnectionManager.isWatchInForeground,       // 워치 화면 활성화 상태
                watchConnectionManager.isPhysicalConnected
            ) { isMobileForeground, isWatchForeground, isPhysicalConnected ->

                Log.d(TAG, "📱 실시간 상태 결합 감지 -> 폰 포어그라운드: $isMobileForeground, 워치 포어그라운드: $isWatchForeground, 워치 물리적 연결: $isPhysicalConnected")

                // 두 상태 중 하나라도 true이면 라이프사이클 세션을 시작하고, 둘 다 꺼지면 종료
                val isUserLoggedIn = authRepository.getCurrentUserUid() != null
                if (!isUserLoggedIn) {
                    lifeCycleManager.stopSession()
                } else if (isMobileForeground || isWatchForeground) {
                    lifeCycleManager.startSession()
                } else {
                    lifeCycleManager.stopSession()
                }
            }.collect()
        }

        // 사용자 데이터 및 위치 관찰 가동
        authRepository.getCurrentUserUid()?.let { myUid ->
            friendRepository.startObservingFriends(myUid)
            startFriendsLocationObservation(myUid)
            initLocationTracker()
            requestWatchStatus()

            scope.launch {
                notificationRepository.startObserving(myUid)
//                notificationRepository.notifications.first { it.isNotEmpty() }.forEach { notification ->
//                    notifiedIds.add(notification.id)
//                }
                observeNotifications()
            }
        }
        // 음성 및 알림 모니터링 가동
        if (voiceObservationJob == null || voiceObservationJob?.isActive == false) {
            observeVoiceMessages()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔴 BackgroundListenerService onDestroy 호출 - 자원 및 세션 정리")

        watchConnectionManager.stopMonitoring()

        // 워치 통신 리스너 해제
        if (::channelClient.isInitialized) {
            channelClient.unregisterChannelCallback(channelCallback)
        }

        // 위치 추적 리스너 안전 해제
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        // 백그라운드 코루틴 작업 취소
        serviceJob.cancel()

        // 라이프사이클 매니저 콜백 해제 및 실시간 세션 강제 종료
        lifeCycleManager.onAppForegroundStatusChanged = null
        lifeCycleManager.stopSession()
    }

    /**
     * 명령 작업 수신 및 액션 라우팅 함수
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BackgroundListenerService onStartCommand 수신")

        // 상주 알림 표시
        try {
            startForegroundServiceNotification()
        } catch (e: Exception) {
            Log.e(TAG, "ForegroundServiceStartNotAllowedException 방어: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        // 수신된 액션별 작업 처리
        intent?.action?.let { action ->
            Log.d(TAG, "⚡ 포어그라운드 서비스 위임 액션 감지: $action")
            when (action) {
                // 친구 위치 정보를 워치로 전송
                ACTION_PUSH_LOCATION_TO_WATCH -> {
                    scope.launch {
                        Log.d(TAG, "🔄 워치의 요청으로 실시간 GPS 강제 새로고침 파이프라인 가동")

                        fetchFreshLocationAndPushToWatch()
                    }
                }
                // 음성 메시지 읽음 처리
                ACTION_UPDATE_VOICE_READ -> {
                    val messageId = intent.getStringExtra(EXTRA_VOICE_MESSAGE_ID)
                    if (!messageId.isNullOrEmpty()) {
                        updateVoiceMessageAsRead(messageId)
                    }
                }
            }
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun fetchFreshLocationAndPushToWatch() {
        scope.launch { // 서비스 내부 CoroutineScope 활용
            try {
                val locationRequest = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY) // GPS 위성 강제 가동
                    .build()

                // 1. 단발성으로 현재 장소의 가장 정확한 좌표를 즉시 측정
                fusedLocationClient.getCurrentLocation(locationRequest, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            scope.launch {
                                // 서버 및 로컬 Repository에 최신 좌표 업데이트 반영
                                val currentCache = liveStatusRepository.getCachedMyLiveStatus()
                                val freshMyStatus = currentCache?.copy(
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        isOnline = true,
                                    )

                                if (freshMyStatus != null) {
                                    // 파이어베이스 Live 컬렉션 및 원격 동기화 진행
                                    liveStatusRepository.updateMyLiveStatus(freshMyStatus)

                                    // 갱신이 완료된 최신 데이터를 워치로 전송
                                    pushLocationsToWatch(listOfNotNull(freshMyStatus) + liveStatusRepository.friendsLiveStatusFlow.value)
                                    Log.d(TAG, "⚡ 워치 요청에 따른 진짜 실시간 GPS 좌표 동기화 및 송신 완료!")
                                }
                            }
                        } else {
                            Log.w(TAG, "⚠️ GPS 측정 결과가 null입니다. 캐시된 마지막 위치로 대체 송신합니다.")
                            // 실패 시 방어 코드로 기존 푸시 로직 가동
                            val cacheMyStatus = liveStatusRepository.getCachedMyLiveStatus()
                            pushLocationsToWatch(listOfNotNull(cacheMyStatus) + liveStatusRepository.friendsLiveStatusFlow.value)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 백그라운드 강제 GPS 갱신 중 실패: ${e.message}")
            }
        }
    }

    /**
     * 음성 메시지 상태 읽음 업데이트 함수
     */
    private fun updateVoiceMessageAsRead(messageId: String) {
        scope.launch {
            // 원격 저장소 읽음 상태 변경
            voiceRepository.markVoiceMessageAsRead(messageId)
                .onSuccess {
                    Log.d(TAG, "✅ [위임 작업] 음성 메시지($messageId) 읽음 처리 완료 성공")
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ [위임 작업] 음성 메시지 읽음 처리 업데이트 실패", e)
                }
        }
    }

    /**
     * 연결된 모든 워치 디바이스 대상 활성화 상태 요청 함수
     */
    private fun requestWatchStatus() {
        scope.launch {
            runCatching {
                // 모든 워치 노드에 상태 확인 메시지 송신
                val nodes = nodeClient.connectedNodes.await()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, PATH_REQUEST_WATCH_STATUS, byteArrayOf())
                        .await()
                }
            }.onFailure { e -> Log.e(TAG, "❌ 워치 상태 요청 실패", e) }
        }
    }

    /**
     * 수신 음성 메시지 모니터링 및 이벤트 분기 함수
     */
    private fun observeVoiceMessages() {
        val isMobileForeground = lifeCycleManager.isAppInForeground.value
        val isWatchForeground = watchConnectionManager.isWatchInForeground.value
        // 기존에 돌고 있는 Job이 있다면 취소하여 중복 구독 방지
        voiceObservationJob?.cancel()

        voiceObservationJob = scope.launch {
            // 인증 상태 확인 및 수신 음성메시지 구독
            authRepository.getAuthStateFlow().collect { uid ->
                if (uid != null) {
                    voiceRepository.observeIncomingVoice(uid, serviceStartTime).collect { voiceMessage ->
                        val url = voiceMessage.voiceUrl

                        // 상황에 맞춰 워치 전송 또는 모바일 이벤트 발생
                        if (url.isNotEmpty() && !voiceMessage.isInitial) {
                            val onlineStatusResult = userRepository.getUserOnlineStatus(uid)
                            if (onlineStatusResult is Result.Success && onlineStatusResult.data) {
                                if (!isMobileForeground && isWatchForeground) {
                                    sendVoiceToWatch(voiceMessage.id, voiceMessage.senderId, url)
                                } else {
                                    voiceRepository.emitMobileVoiceEvent(voiceMessage)
                                }
                            } else {
                                Log.d(TAG, "사용자가 오프라인 상태이거나 상태 조회에 실패하여 이벤트를 건너뜁니다.")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 음성 재생 페이로드 가공 및 워치 메시지 송신 함수
     */
    private fun sendVoiceToWatch(messageId: String, senderId: String, voiceUrl: String) {
        Log.d(TAG, "워치로 음성 전송중..")
        scope.launch {
            runCatching {
                // 발신자 정보 및 메시지 데이터 구성
                val senderFriend = friendRepository.myFriends.value.find { it.uid == senderId }
                val payload = mapOf(
                    "messageId" to messageId,
                    "voiceUrl" to voiceUrl,
                    "senderName" to (senderFriend?.nickname ?: "알 수 없음"),
                    "senderProfileImage" to (senderFriend?.profileImageUrl ?: "")
                )

                // 데이터 직렬화 후 모든 워치 기기로 송신
                val byteArray = Gson().toJson(payload).toByteArray(Charsets.UTF_8)
                val nodes = nodeClient.connectedNodes.await()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, PATH_PLAY_VOICE, byteArray).await()
                }
            }.onFailure { e -> Log.e(TAG, "❌ 워치로 음성 전송 실패", e) }
        }
    }

    /**
     * 오디오 파일 압축 인코딩 처리 함수
     */
    private fun encodePcmToM4a(pcmFile: File, outputFile: File) {
        // 인코더 설정용 포맷 조건 정의
        val sampleRate = 16000
        val channels = 1
        val bitRate = 64000
        val timeoutUs = 10000L

        val format =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels)
                .apply {
                    setInteger(
                        MediaFormat.KEY_AAC_PROFILE,
                        MediaCodecInfo.CodecProfileLevel.AACObjectLC
                    )
                    setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
                }

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var fis: FileInputStream? = null
        var isMuxerStarted = false

        try {
            // 오디오 인코더 및 파일 저장소 인프라 준비
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            fis = FileInputStream(pcmFile)

            val bufferInfo = MediaCodec.BufferInfo()
            val readBuffer = ByteArray(2048)
            var audioTrackIndex = -1
            var isPcmEOS = false
            var isCodecEOS = false
            var presentationTimeUs = 0L

            // 원시 데이터를 가져와 압축 포맷으로 인코딩 반복 처리
            while (!isCodecEOS) {
                // 입력 파일에서 바이트 데이터를 읽어 인코더 큐에 삽입
                if (!isPcmEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val bytesRead = fis.read(readBuffer)
                            if (bytesRead == -1) {
                                isPcmEOS = true
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    0,
                                    presentationTimeUs,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                            } else {
                                inputBuffer.put(readBuffer, 0, bytesRead)
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    bytesRead,
                                    presentationTimeUs,
                                    0
                                )
                                presentationTimeUs += (bytesRead * 1_000_000L) / (sampleRate * channels * 2)
                            }
                        }
                    }
                }

                // 인코딩 완료된 데이터를 파일 저장소 트랙에 기록
                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size > 0 && isMuxerStarted && outputBuffer != null) {
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

                // 포맷 변경 확인 시 저장소 라이팅 시작
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!isMuxerStarted) {
                        audioTrackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        isMuxerStarted = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "인코딩 파이프라인 에러", e)
        } finally {
            // 인코더 중단 및 스트림 자원 해제
            runCatching { fis?.close() }
            runCatching {
                codec?.stop()
                codec?.release()
            }
            runCatching {
                if (isMuxerStarted) muxer?.stop()
                muxer?.release()
            }
        }
    }

    /**
     * 워치 스트리밍 파일 데이터 수신 및 결합 처리 함수
     */
    private fun receiveWatchAudio(channel: ChannelClient.Channel) {
        scope.launch {
            val pcmFile = File(cacheDir, "walkie_talkie.pcm")
            val m4aFile = File(cacheDir, "walkie_talkie.m4a")
            val startTime = System.currentTimeMillis()
            val targetUid = channel.path.substringAfter("$PATH_AUDIO_STREAM_PREFIX/", "").trim()

            // 타깃 사용자 유효성 검증
            if (targetUid.isEmpty()) {
                runCatching { channelClient.close(channel).await() }
                return@launch
            }

            try {
                // 기존 파일 제거
                if (pcmFile.exists()) pcmFile.delete()
                if (m4aFile.exists()) m4aFile.delete()

                // 워치로부터 유입되는 오디오 스트림을 임시 파일로 수신 저장
                val inputStream = Tasks.await(channelClient.getInputStream(channel))
                inputStream.use { input ->
                    FileOutputStream(pcmFile, false).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: ChannelIOException) {
                Log.w(TAG, "⚠️ 워치 채널 전송 마감 중 끊김")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 오디오 수신 오류", e)
            } finally {
                // 수신 채널 종료 후 인코딩 변환 작업 및 서버 발송 연계
                runCatching {
                    channelClient.close(channel).await()
                    if (pcmFile.exists() && pcmFile.length() > 0) {
                        encodePcmToM4a(pcmFile, m4aFile)
                        val duration =
                            (((System.currentTimeMillis() - startTime) / 1000).toInt()).coerceAtLeast(
                                1
                            )
                        if (m4aFile.exists() && m4aFile.length() > 0) {
                            sendWatchAudioToServer(targetUid, Uri.fromFile(m4aFile), duration)
                        }
                        pcmFile.delete()
                    }
                }
            }
        }
    }

    /**
     * 서버 업로드 및 다이렉트 음성 메시지 발송 함수
     */
    private fun sendWatchAudioToServer(targetUid: String, fileUri: Uri, duration: Int) {
        scope.launch {
            val senderUid = authRepository.getCurrentUserUid() ?: return@launch

            // 파일 업로드 성공 후 음성 메시지 최종 전송
            voiceRepository.uploadVoiceFile(fileUri)
                .onSuccess { url ->
                    voiceRepository.sendVoiceMessage(targetUid, url, duration)
                }
                .onFailure { Log.e(TAG, "파일 전송 실패") }
        }
    }

    /**
     * 내 위치 및 친구 라이브 상태 결합 스트림 관찰 함수
     */
    private fun startFriendsLocationObservation(myUid: String) {
        scope.launch {
            // 위치 변경 데이터를 통합 수집하여 워치 전송으로 연계
            liveStatusRepository.observeFriendsLiveStatus(myUid)
            combine(
                liveStatusRepository.myLiveStatusFlow,
                liveStatusRepository.friendsLiveStatusFlow
            ) { myStatus, friendsList ->
                listOfNotNull(myStatus) + friendsList
            }.collect { totalLocations ->
                pushLocationsToWatch(totalLocations)
            }
        }
    }

    /**
     * 최신 위치 좌표 데이터 구조체 워치 송신 함수
     */
    private fun pushLocationsToWatch(locations: List<LiveStatus>) {
        val isWatchForeground = watchConnectionManager.isWatchInForeground.value
        if(!isWatchForeground) return
        scope.launch {
            runCatching {
                // 데이터를 직렬화하여 연결된 워치 기기들에 송신
                val jsonPayload = Gson().toJson(locations)
                val byteArray = jsonPayload.toByteArray(Charsets.UTF_8)
                val nodes = nodeClient.connectedNodes.await()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, PATH_RESPONSE_FRIENDS_LOCATION, byteArray)
                        .await()
                }
            }.onFailure { e -> Log.e(TAG, "❌ 워치 위치 푸시 에러", e) }
        }
    }

    /**
     * 위치 요청 파라미터 빌드 및 추적 옵저버 초기화 함수
     */
    private fun initLocationTracker() {
        // 위치 추적 클라이언트 빌드 및 콜백 수신기 설정
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                scope.launch {
                    val myUid = authRepository.getCurrentUserUid() ?: return@launch
                    for (location in locationResult.locations) {
                        processLocationUpdate(myUid, location.latitude, location.longitude)
                    }
                }
            }
        }

        // 초기 위치 동기화 및 실시간 추적 시작
        fetchLastKnownLocationAndSync()
        startLocationUpdates(locationRequest)
    }

    /**
     * 로컬 장치 최종 기록 좌표 확인 및 동기화 유발 함수
     */
    @SuppressLint("MissingPermission")
    private fun fetchLastKnownLocationAndSync() {
        scope.launch {
            val myUid = authRepository.getCurrentUserUid() ?: return@launch

            // 캐시 기록 기반의 기기 현재 위치 안전 추출 및 서버 전송
            runCatching {
                fusedLocationClient.lastLocation.await()?.let { location ->
                    processLocationUpdate(myUid, location.latitude, location.longitude)
                }
            }.onFailure { e -> Log.e(TAG, "초기 위치 확보 실패: ${e.message}") }
        }
    }

    /**
     * 위치 데이터 가공 및 원격 서버 동기화 함수
     */
    private suspend fun processLocationUpdate(myUid: String, latitude: Double, longitude: Double) {
        // 기존 상태 값을 가져와 좌표 정보 데이터 복사 최신화
        val currentStatus = liveStatusRepository.getCachedMyLiveStatus()
            ?: (liveStatusRepository.getLiveStatusByUid(myUid) as? com.bbip.bbipit.core.result.Result.Success)?.data

        val updatedLiveStatus = currentStatus?.copy(latitude = latitude, longitude = longitude)
            ?: LiveStatus(uid = myUid, latitude = latitude, longitude = longitude)

        liveStatusRepository.updateMyLiveStatus(updatedLiveStatus)
    }

    /**
     * 시스템 위치 프로바이더 엔진 가동 등록 함수
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(request: LocationRequest) {
        // 시스템 내부 위치 관리 인터페이스에 콜백 가동 등록
        runCatching {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        }.onFailure { Log.e(TAG, "위치 추적 시작 실패: ${it.message}") }
    }

    /**
     * 시스템 알림 채널 정의 및 서비스 상주 알림 등록 함수
     */
    private fun startForegroundServiceNotification() {
        val channelId = CHANNEL_ID_VOICE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "워치 무전 수신 상주 서비스",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bipp-it 대기 중")
            .setContentText("워치로부터 음성 신호를 받을 준비가 되었습니다.")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // 최신 안드로이드 버전에 따른 필수 실행 유형 명시 설정 분기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                // 모든 백그라운드 무전/위치 동기화 타입으로 완벽 기동 시도
                startForeground(
                    1, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
                Log.d(TAG, "✅ 모든 FGS 멀티 타입 지정하여 서비스 정상 가동")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 블루투스 등 특정 권한 미부여로 복합 FGS 시작 실패, DATA_SYNC 단독 타입으로 안전 전환합니다: ${e.message}")
                try {
                    // DATA_SYNC 단독 타입으로 기동
                    startForeground(
                        1, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } catch (e2: Exception) {
                    Log.w(TAG, "⚠️ DATA_SYNC 타입 가동 실패, 기본 무타입 포어그라운드로 최종 전환합니다: ${e2.message}")
                    try {
                        //무타입 기본 포어그라운드로 최종 폴백
                        startForeground(1, notification)
                    } catch (e3: Exception) {
                        Log.e(TAG, "❌ 모든 방식의 Foreground Service 가동 실패", e3)
                        throw e3
                    }
                }
            }
        } else {
            startForeground(1, notification)
        }
    }

    /**
     * Repository 캐시 구독 기반 신규 알림 감지 및 시스템 알림 발행 함수
     * 초기 수신 전체 문서는 notifiedIds에 등록만 하고 알림 발행 없이 스킵
     * 이후 추가된 신규 문서만 시스템 알림으로 발행
     * WALKIE 타입은 앱 백그라운드 상태일 때만 시스템 알림 발행
     */
    private fun observeNotifications() {
        scope.launch {
            // 서비스 시작 시점 이후에 생성된 알림만 처리
            val serviceStartTime = System.currentTimeMillis()
            notificationRepository.notifications.collect { notifications ->
                notifications.forEach { notification ->
                    if (!notification.isRead &&
                        !notifiedIds.contains(notification.id)
                    ) {
                        Log.d(
                            TAG,
                            "알림 감지 - id: ${notification.id}, type: ${notification.type}, isInitial: ${notification.isInitial}"
                        )
                        notifiedIds.add(notification.id)
                        Log.d(
                            TAG,
                            "🔔 신규 알림 감지 및 중복 차단 등록: ${notification.id} (타입: ${notification.type})"
                        )

                        if (!notification.isInitial) {
                            // WALKIE 타입 최우선 분기 처리
                            if (notification.type == "WALKIE") {
                                // 1. 앱이 켜져있을 때 (포그라운드) -> 화면 안에서 바로 무전 자동 재생
                                if (lifeCycleManager.isAppInForeground.value) {
                                    val currentUserId =
                                        authRepository.getCurrentUserUid() ?: return@forEach
                                    scope.launch {
                                        Log.d(TAG, "🔊 앱 포그라운드 상태 -> 무전 즉시 자동 재생 구동")
                                        notificationRepository.playWalkieNotification(
                                            notification = notification,
                                            receiverId = currentUserId
                                        )
                                    }
                                }
                                // 2. 앱이 꺼져있거나 홈화면일 때 (백그라운드)
                                else {
                                    Log.d(TAG, "📱 앱 백그라운드 상태 -> 시스템 팝업 배너만 표출")
                                    showSystemNotification(notification)
                                }
                                // WALKIE는 여기서 처리를 끝내고 다른 알림 로직으로 넘어가지 않게 방어
                                return@forEach
                            }
                            // 일반 알림(DM, REQ) 처리
                            showSystemNotification(notification)
                        }
                    }
                }
            }
        }
    }

    /**
     * 안드로이드 시스템 알림 채널 구성 및 사용자 대상 헤즈업(Heads-up) 알림 표시 함수
     */
    private fun showSystemNotification(notification: com.bbip.bbipit.domain.entity.Notification) {
        Log.d(TAG, "WALKIE 배너 발행 - audioId: ${notification.audioId}")
        Log.d(TAG, "🔔 showSystemNotification 호출: ${notification.type}, ${notification.senderName}")
        val channelId = "phone_alert_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 오레오(API 26) 이상 대응용 알림 채널 생성 및 중요도(HIGH) 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "중요 알림"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val bodyText = when (notification.type) {
            "REQ" -> "친구 요청이 왔습니다."
            "ACT" -> "친구 요청이 수락되었습니다."
            "WALKIE" -> "무전이 왔습니다."
            else -> notification.content
        }

        val safeFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // 배너 클릭 시 Intent
        val intent = when (notification.type) {
            "DM" -> Intent(this, MainActivity::class.java).apply {
                flags = safeFlags
                putExtra("notification_type", "DM")
                putExtra("notification_id", notification.id)
                putExtra("notification_room_id", notification.roomId)
                putExtra("notification_receiver_id", notification.senderId)
            }

            "REQ" -> Intent(this, MainActivity::class.java).apply {
                flags = safeFlags
                putExtra("notification_type", "REQ")
                putExtra("notification_id", notification.id)
            }

            "ACP" -> Intent(this, MainActivity::class.java).apply {
                flags = safeFlags
                putExtra("notification_type", "ACP")
                putExtra("notification_id", notification.id)
            }

            "WALKIE" -> Intent(this, MainActivity::class.java).apply {
                flags = safeFlags
                putExtra("notification_type", "WALKIE")
                putExtra("notification_id", notification.id)
                putExtra("notification_audio_id", notification.audioId)
                putExtra("notification_sender_id", notification.senderId)
                putExtra("notification_created_at", notification.createdAt)
            }

            else -> Intent(this, MainActivity::class.java).apply {
                flags = safeFlags
            }
        }

        Log.d(
            TAG,
            "WALKIE Intent extras - type: ${intent.getStringExtra("notification_type")}, id: ${
                intent.getStringExtra("notification_id")
            }"
        )

        // 알림 클릭 시 Intent
        val pendingIntent = PendingIntent.getActivity(
            this,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // 시스템 알림 빌더 구동 및 인텐트 파라미터 기반 시각적 요소 구성
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.bbip.bbipit.R.drawable.baseline_notifications_24)
            .setContentTitle(notification.senderName)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // 고유 ID 기반 시스템 서비스 알림 발행
        val notificationId = notification.id.hashCode()
        notificationManager.notify(notificationId, builder.build())
    }
}