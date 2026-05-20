package com.bbip.bbipit.core.base

import com.google.android.gms.common.api.ApiException
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.data.repository.UserRepositoryImpl
import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import com.bbip.bbipit.domain.usecase.SyncMyLocationUseCase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.ChannelClient.Channel
import com.google.android.gms.wearable.ChannelClient.ChannelCallback
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * 백그라운드 위치 추적, 워치 통신, 무전 수신 지속용 포어그라운드 서비스 클래스
 */
@AndroidEntryPoint
class BackgroundListenerService : Service() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var voiceRepository: VoiceRepository
    @Inject lateinit var liveStatusRepository: LiveStatusRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var syncMyLocationUseCase: SyncMyLocationUseCase
    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver
    @Inject lateinit var lifeCycleManager: LifeCycleManager

    // 비동기 작업 및 스트림 구독 통제용 서비스 전역 코루틴 스코프
    private val scope = CoroutineScope(Dispatchers.IO)
    private val TAG = "BackgroundListenerService"

    // 구글 위치 서비스 API 연동용 클라이언트 및 콜백 객체
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // 웨어러블 기기 데이터 송수신용 클라이언트 객체 지연 초기화
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val nodeClient by lazy { Wearable.getNodeClient(this) }
    private lateinit var channelClient: ChannelClient

    // 워치 앱 화면의 포어그라운드 활성화 상태 플래그 변수
    private var isWatchInForeground = false

    /**
     * 워치측 화면 상태 변경 이벤트 수신용 메시지 리스너
     * 바이트 데이터의 불리언 변환을 통한 플래그 갱신 및 상태별 세션 제어 파이프라인 구동 목적
     */
    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        if (messageEvent.path == "/watch_state") {
            val state = String(messageEvent.data).toBoolean()
            isWatchInForeground = state
            Log.d(TAG, "⌚ 워치 상태 변경 감지 -> 포어그라운드 여부: $state")

            // 워치 상태 변화에 맞춘 하트비트 세션 활성화 여부 재결정
            manageSessionByState()
        }
    }

    // 바인딩 서비스 미사용에 따른 null 반환
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 서비스 최초 생성 시점 시스템 호출 초기화 콜백 함수
     * 웨어러블 채널 및 메시지 리스너 등록, 초기 위치 추적 및 워치 상태 동기화 요청 목적
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BackgroundListenerService onCreate")

        channelClient = Wearable.getChannelClient(this)
        channelClient.registerChannelCallback(channelCallback)

        Wearable.getMessageClient(this).addListener(messageListener)

        val myUid = authRepository.getCurrentUserUid()
        if (myUid != null) {
            // 로그인 상태 확인 후 친구 위치 관찰 및 폰 자체 위치 추적 개시
            startFriendsLocationObservation(myUid)
            initLocationTracker()

            // 서비스 구동 시점 워치 측 대상 현재 화면 활성화 상태 파악용 쿼리 송신
            requestWatchStatus()

            // 초기 상태 조합 기반 하트비트 세션 상태 평가
            manageSessionByState()
        }

        // 실시간 무전 신호 수신 처리용 관찰 흐름 개시
        observeVoiceMessages()
    }

    /**
     * 서비스 명령 전달 시점 호출 콜백 함수
     * 시스템 알림 표시를 통한 포어그라운드 권한 유지 및 앱 라이프사이클 옵저버 트리거 대응 세션 관리 목적
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BackgroundListenerService onStartCommand")

        try {
            // 안드로이드 운영체제 정책 부합용 포어그라운드 서비스 알림 표출
            startForegroundServiceNotification()
        } catch (e: Exception) {
            // 포어그라운드 서비스 시작 금지 예외 발생 시 서비스 안전 자체 종료 처리
            Log.e(TAG, "ForegroundServiceStartNotAllowedException 방어: ${e.message}")
            stopSelf()
        }

        // 폰 내부 화면 전환 및 워치 신호 변경 시 최신 상태 취합 기반 세션 제어
        manageSessionByState()

        // 시스템 강제 종료 시 가용 상태로 즉시 재시작하도록 설정 (START_STICKY)
        return START_STICKY
    }

    /**
     * 페어링된 모든 워치 노드 탐색 및 현재 워치 앱 구동 상태 확인 요청 전송 함수
     */
    private fun requestWatchStatus() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/request_watch_status", byteArrayOf()).await()
                    Log.d(TAG, "🔄 워치(${node.displayName})에게 현재 상태 확인 요청 송신")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 워치 상태 요청 실패", e)
            }
        }
    }

    /**
     * 모바일 및 워치 기기 화면 활성화 상태 종합 기반 실시간 서버 동기화 세션 통제 단일 파이프라인 함수
     */
    private fun manageSessionByState() {
        val isMobileForeground = appLifecycleObserver.isAppInForeground
        val isUserLoggedIn = authRepository.getCurrentUserUid() != null

        // 비로그인 사용자의 세션 가동 원천 차단 및 기존 세션 파기 처리
        if (!isUserLoggedIn) {
            Log.d(TAG, "🛑 로그아웃 상태 -> 세션 종료")
            lifeCycleManager.stopSession()
            return
        }

        // 모바일 기기 혹은 워치 기기 활성화 시 세션 가동, 양측 모두 비활성화 시 중지 처리
        if (isMobileForeground || isWatchInForeground) {
            Log.d(TAG, "🔄 세션 조건 충족 [Start] -> (폰 포그라운드: $isMobileForeground, 워치 포그라운드: $isWatchInForeground)")
            lifeCycleManager.startSession()
        } else {
            Log.d(TAG, "😴 둘 다 백그라운드 진입 [Stop] -> (폰 포그라운드: $isMobileForeground, 워치 포그라운드: $isWatchInForeground)")
            lifeCycleManager.stopSession()
        }
    }

    /**
     * 서비스 파괴 시점 리소스 정리 콜백 함수
     * 친구 목록 관찰 중단, 등록 리스너 해제, 코루틴 작업 전면 취소 목적
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BackgroundListenerService onDestroy")

        if (userRepository is UserRepositoryImpl) {
            (userRepository as UserRepositoryImpl).stopObservingFriends()
        }

        // 세션 안전 중지 및 하드웨어 자원, 메시지 클라이언트 연결 해제
        lifeCycleManager.stopSession()
        Wearable.getMessageClient(this).removeListener(messageListener)

        if(::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        scope.cancel()
    }

    /**
     * 워치 대용량 데이터 채널 개방 시점 실시간 음성 스트림 경로 판별 및 데이터 수신 개시 콜백 객체
     */
    private val channelCallback = object : ChannelCallback() {
        override fun onChannelOpened(channel: Channel) {
            if (channel.path.startsWith("/audio_stream")) {
                receiveWatchAudio(channel)
            }
        }
    }

    // 라이프사이클 옵저버 플래그 역전 기반 스마트폰 백그라운드 위치 여부 반환 판별 함수
    private fun isMobileBackground(): Boolean {
        return !appLifecycleObserver.isAppInForeground
    }

    /**
     * 파이어베이스 서버 수신 무전 메시지 실시간 감시 함수
     * 스마트폰 백그라운드 상태 혹은 워치 구동 시 워치로 가공 데이터 전달, 그 외 모바일 직접 이벤트 발행 처리 목적
     */
    private fun observeVoiceMessages() {
        scope.launch {
            authRepository.getAuthStateFlow().collectLatest { uid ->
                if (uid != null) {
                    voiceRepository.observeIncomingVoice(uid).collect { voiceMessage ->
                        val url = voiceMessage.voiceUrl
                        if (url.isNotEmpty() && !voiceMessage.isRead) {
                            if (isMobileBackground() && isWatchInForeground) {
                                sendVoiceToWatch(voiceMessage.id, voiceMessage.senderId, url)
                            } else {
                                voiceRepository.emitMobileVoiceEvent(voiceMessage)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 수신 무전 메시지 발신자 프로필 및 정보 데이터 취합 기반 워치 기기 원격 재생 명령 패킷 전송 함수
     */
    private fun sendVoiceToWatch(messageId: String, senderId: String, voiceUrl: String) {
        scope.launch {
            try {
                val senderFriend = userRepository.myFriends.value.find { it.uid == senderId }
                val senderName = senderFriend?.nickname ?: "알 수 없음"
                val senderProfileImage = senderFriend?.profile_image_url ?: ""

                val payload = mapOf(
                    "messageId" to messageId,
                    "senderId" to senderId,
                    "voiceUrl" to voiceUrl,
                    "senderName" to senderName,
                    "senderProfileImage" to senderProfileImage
                )
                val byteArray = Gson().toJson(payload).toByteArray(Charsets.UTF_8)

                val nodes = nodeClient.connectedNodes.await()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/play_voice", byteArray).await()
                    Log.d(TAG, "✅ [무전 수신] 워치(${node.displayName})로 음성 주소 전송 완료")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 워치로 음성 전송 실패", e)
            }
        }
    }

    /**
     * 워치 송신 생 오디오 로우 파일(PCM) 대상 범용 압축 포맷(AAC 계열 M4A) 고속 로우레벨 인코딩 변환 함수
     */
    private fun encodePcmToM4a(pcmFile: File, outputFile: File) {
        val sampleRate = 16000
        val channels = 1
        val bitRate = 64000
        val timeoutUs = 10000L

        // AAC Low Complexity 프로필 대응 오디오 포맷 구조 빌드
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        // 인코딩 스트림 데이터의 물리 컨테이너 파일 패킹용 멀티플렉서(Muxer) 세팅
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
            while (!isCodecEOS) {
                // 원본 PCM 바이트 배열의 코덱 입력 인덱스 큐 순차 공급 처리
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
                            // 오디오 샘플 크기 및 가용 채널 정보 수식화 기반 타임스탬프 보정
                            presentationTimeUs += (bytesRead * 1_000_000L) / (sampleRate * channels * 2)
                        }
                    }
                }

                // 인코더 출력 버퍼 내 변환 완료 부호화 데이터 추출 및 파일 멀티플렉서 기록 처리
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

                // 출력 포맷 변경 감지 최초 시점의 멀티플렉서 트랙 개설 및 미디어 스트림 작성 시작 처리
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!isMuxerStarted) {
                        audioTrackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        isMuxerStarted = true
                    }
                }
            }
        } finally {
            // 작업 성공 여부 무관 시스템 입출력 자원 및 코덱 컴포넌트 안전 릴리즈 처리
            try { fis.close() } catch (e: Exception) { Log.d(TAG, e.toString()) }
            try { codec.stop(); codec.release() } catch (e: Exception) { e.printStackTrace() }
            try { if (isMuxerStarted) muxer.stop(); muxer.release() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /**
     * 워치 송신 원격 입력 스트림 채널 개방, 캐시 파일 임시 저장, 완료 시 파일 변환 파이프라인 위임 함수
     */
    private fun receiveWatchAudio(channel: Channel) {
        Log.d("AudioService", "워치 음성 채널 연결 및 데이터 수신 시작")
        scope.launch {
            try {
                val pcmFile = File(cacheDir, "walkie_talkie.pcm")
                val m4aFile = File(cacheDir, "walkie_talkie.m4a")

                if (pcmFile.exists()) pcmFile.delete()
                if (m4aFile.exists()) m4aFile.delete()

                val inputStream = com.google.android.gms.tasks.Tasks.await(channelClient.getInputStream(channel))
                val startTime = System.currentTimeMillis()

                // 입력 스트림 버퍼 순회 기반 하드디스크 영역 바이트 로우 데이터 블록 동기식 출력 처리
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
                channelClient.close(channel).await()

                if (pcmFile.exists() && pcmFile.length() > 0) {
                    // PCM 파일 작성 완료 시점의 M4A 고압축 포맷 인코딩 위임
                    encodePcmToM4a(pcmFile, m4aFile)
                    val durationSeconds = ((endTime - startTime) / 1000).toInt().coerceAtLeast(1)

                    if (m4aFile.exists() && m4aFile.length() > 0) {
                        // 정상 생성 미디어 파일 객체의 중앙 백엔드 서버 최종 전송 처리
                        sendWatchAudioToServer(Uri.fromFile(m4aFile), durationSeconds)
                    }
                    pcmFile.delete()
                }
            } catch (e: Exception) {
                Log.e("AudioService", "오디오 수신 처리 중 오류 발생", e)
            }
        }
    }

    /**
     * 로컬 스토리지 저장 오디오 미디어 파일 URI 기반 원격 스토리지 업로드 및 최종 메시지 전송 함수
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
     * 친구 상태 변화 및 내 라이브 상태 병합 기반 관찰 경로 수립 관찰 초기화 함수
     */
    private fun startFriendsLocationObservation(myUid: String) {
        scope.launch {
            userRepository.startObservingFriends(myUid)
            liveStatusRepository.observeFriendsLiveStatus(myUid)

            // 내 상태 데이터 스트림 및 주변인 상태 데이터 스트림 실시간 결합 목적
            combine(
                liveStatusRepository.myLiveStatusFlow,
                liveStatusRepository.friendsLiveStatusFlow
            ) { myStatus, friendsList ->
                val totalList = mutableListOf<LiveStatus>()
                myStatus?.let { totalList.add(it) }
                totalList.addAll(friendsList)
                totalList
            }.collect { totalLocationsList ->
                // 데이터 변동 시점의 페어링 워치 화면 대상 최신 위치 배열 패킷 푸시 처리
                pushLocationsToWatch(totalLocationsList)
            }
        }
    }

    /**
     * 스마트폰 동기화 사용자 위치 목록 대상 JSON 가공 및 무선 연결 워치 노드 송신 함수
     */
    private fun pushLocationsToWatch(locations: List<LiveStatus>) {
        scope.launch {
            try {
                val jsonPayload = Gson().toJson(locations)
                val byteArray = jsonPayload.toByteArray(Charsets.UTF_8)
                val nodes = nodeClient.connectedNodes.await()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/response_friends_location", byteArray).await()
                }
            } catch (e: ApiException) {
                if (e.statusCode == 17) { // 17: CommonStatusCodes.API_UNAVAILABLE
                    Log.d(TAG, "워치 API 미지원 기기이므로 워치 연동 작업 생략")
                } else {
                    Log.e(TAG, "❌ 워치 통신 에러", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 워치 통신 에러", e)
            }
        }
    }

    /**
     * 디바이스 GPS 추적 모듈 주기 및 정확도 설정, 최초 단기 위치 동기화 수행 초기화 작업 함수
     */
    private fun initLocationTracker() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

        createLocationCallback()
        fetchLastKnownLocationAndSync()
        startLocationUpdates(locationRequest)
    }

    /**
     * 장치 기록 최신 유효 위치 정보 조회 기반 첫 동기화 누락 리스크 방어용 안전 예방 함수
     */
    @SuppressLint("MissingPermission")
    private fun fetchLastKnownLocationAndSync() {
        scope.launch {
            val myUid = authRepository.getCurrentUserUid() ?: return@launch
            try {
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null) {
                    processLocationUpdate(myUid, lastLocation.latitude, lastLocation.longitude)
                }
            } catch (e: Exception) {
                Log.e(TAG, "초기 위치 확보 실패: ${e.message}")
            }
        }
    }

    /**
     * 측정 위경도 좌표 기반 도메인 모델 가공 및 서버 공간 최종 업로드 정합성 확보용 데이터 처리 함수
     */
    private suspend fun processLocationUpdate(myUid: String, latitude: Double, longitude: Double) {
        var currentStatus = liveStatusRepository.getCachedMyLiveStatus()
        if (currentStatus == null) {
            val result = liveStatusRepository.getLiveStatusByUid(myUid)
            if (result is com.bbip.bbipit.core.result.Result.Success) {
                currentStatus = result.data
            }
        }

        val updatedLiveStatus = currentStatus?.copy(
            latitude = latitude,
            longitude = longitude,
        ) ?: LiveStatus(uid = myUid, latitude = latitude, longitude = longitude)

        syncMyLocationUseCase(updatedLiveStatus)
    }

    /**
     * GPS 하드웨어 모듈 주기 반환 실제 위경도 패킷 감지용 내부 콜백 인스턴스 설계 함수
     */
    private fun createLocationCallback() {
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
    }

    /**
     * 구글 플레이 위치 클라이언트 내 예약 콜백 및 갱신 요청 결합 등록 기반 런타임 추적 루프 구동 함수
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(request: LocationRequest) {
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("GPS_SERVICE", "위치 권한 거부: ${e.message}")
        }
    }

    /**
     * 운영체제 버전별 요구 상한선 준수 알림 채널 구성 및 지속 백그라운드 연산 자원 확보용 서비스 고정 함수
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
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setOngoing(true)
            .build()

        // 안드로이드 14(Upside Down Cake, API 34) 이상 전제 필수 포어그라운드 유형 명시 가이드 우회 적용 처리
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    1, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } catch (e: android.app.ForegroundServiceStartNotAllowedException) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            }
        } else {
            startForeground(1, notification)
        }
    }
}