package com.bbip.bbipit

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.data.WatchDataRepository
import com.bbip.bbipit.models.MobileServiceStatus
import com.bbip.bbipit.models.WatchLiveStatus
import com.bbip.bbipit.models.WatchVoiceData
import com.bbip.bbipit.notification.WatchNotificationHelper
import com.bbip.bbipit.util.VoiceEventBus
import com.bbip.bbipit.util.WatchAudioPlayer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.collections.get

/**
 * 메인 화면 UI 상태 관리 뷰모델
 */
class WatchMainViewModel(application: Application) :
    AndroidViewModel(application),
    MessageClient.OnMessageReceivedListener {

    val TAG = "WatchMainViewModel"

    var isWatchActiveInForeground: Boolean = false

    // 음성 메시지 수신 상태 플로우
    private val _voiceUiState = MutableStateFlow<WatchVoiceData?>(null)

    // 음성 수신 상태 스트림
    val voiceUiState = _voiceUiState.asStateFlow()

    private val audioPlayer = WatchAudioPlayer.getInstance(application)

    // 휴대폰 백그라운드 서비스 상태를 관리하는 StateFlow (기본값은 확인 중)
    private val _mobileStatus = MutableStateFlow(MobileServiceStatus.CHECKING)
    val mobileStatus: StateFlow<MobileServiceStatus> = _mobileStatus.asStateFlow()

    // 휴대폰 서비스 상태 수신을 공유할 이벤트 버스
    private val _mobileStatusEventBus = MutableSharedFlow<String>(replay = 0)
    val mobileStatusEventBus = _mobileStatusEventBus.asSharedFlow()

    private val messageClient = Wearable.getMessageClient(application)
    private val nodeClient = Wearable.getNodeClient(application)

    init {

        Wearable.getMessageClient(application).addListener(this)

        // 음성 수신 이벤트 구독 및 상태 업데이트
        viewModelScope.launch {
            VoiceEventBus.incomingVoiceEvent.collect { data ->
                if (data != null) {
                    _voiceUiState.value = data
                    playVoice(data)
                }
            }
        }
    }

    private fun playVoice(voiceData: WatchVoiceData) {
        audioPlayer.playFromUrl(voiceData.voiceUrl) {
            Log.d("WatchMainViewModel", "🎵 오디오 재생 완료: ${voiceData.messageId}")

            // UI 닫기
            _voiceUiState.value = null

            // 스마트폰으로 읽음 완료 패킷 전송
            viewModelScope.launch(Dispatchers.IO) {
                sendReadStatusToPhone(voiceData.messageId)
            }
        }
    }

    private suspend fun sendReadStatusToPhone(messageId: String) {
        try {
            val context = getApplication<Application>()
            val nodeClient = Wearable.getNodeClient(context)
            val messageClient = Wearable.getMessageClient(context)
            val nodes = nodeClient.connectedNodes.await()
            val phoneNode = nodes.firstOrNull()

            phoneNode?.let {
                messageClient.sendMessage(
                    it.id,
                    "/mark_voice_read",
                    messageId.toByteArray(Charsets.UTF_8)
                ).await()
                Log.d("WatchMainViewModel", "✅ 스마트폰으로 읽음 신호 전송 완료")
            }
        } catch (e: Exception) {
            Log.e("WatchMainViewModel", "❌ 읽음 신호 전송 실패", e)
        }
    }

    /**
     * 휴대폰 백그라운드 서비스 상태 확인 요청 및 타임아웃 검증
     */
    fun checkPhoneServiceStatus() {
        viewModelScope.launch {
            _mobileStatus.value = MobileServiceStatus.CHECKING

            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.e(TAG, "❌ 연결된 휴대폰 디바이스가 없습니다.")
                    _mobileStatus.value = MobileServiceStatus.SERVICE_RESTRICTED
                    return@launch
                }

                // 휴대폰으로 상태 체크 신호 송신
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/check_phone_status", byteArrayOf()).await()
                }
                Log.d(TAG, "📲 휴대폰으로 상태 신호 송신 후 응답 대기 시작 (타임아웃 3초)")

                // 3초 내에 WatchCentralService의 이벤트 버스로 신호가 오는지 대기합
                val resultStatus = withTimeoutOrNull(3000L) {
                    mobileStatusEventBus.first()
                }

                // 결과에 따른 상태 제어
                if (resultStatus == null) {
                    // 3초 동안 휴대폰 서비스로부터 아무런 응답이 오지 않은 경우 (프로세스 Dead 상태 등)
                    Log.w(TAG, "⏳ 휴대폰 응답 타임아웃 초과! 서비스를 제한합니다.")
                    _mobileStatus.value = MobileServiceStatus.SERVICE_RESTRICTED
                } else {
                    // 성공적으로 응답을 수신한 경우
                    Log.d(TAG, "📱 최종 수신된 휴대폰 상태 상태: $resultStatus")
                    if (resultStatus == "READY") {
                        _mobileStatus.value = MobileServiceStatus.READY
                    } else {
                        _mobileStatus.value = MobileServiceStatus.SERVICE_RESTRICTED
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 상태 체크 프로세스 중 에러 발생", e)
                _mobileStatus.value = MobileServiceStatus.SERVICE_RESTRICTED
            }
        }
    }

    /**
     * 음성 데이터 상태 초기화
     */
    fun clearVoiceState() {
        _voiceUiState.value = null
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // 로그를 추가하여 패킷 수신 여부 확인 디버깅 용이화
        Log.d(TAG, "📥 [MainViewModel] 패킷 수신. Path: ${messageEvent.path}")
        when (messageEvent.path) {
            "/request_watch_status" -> {
                viewModelScope.launch { sendWatchStateToPhone(isWatchActiveInForeground) }
            }
            "/play_voice" -> {
                handleIncomingVoiceMessage(messageEvent)
            }
            "/phone_status_reply" -> {
                try {
                    val replyStatus = String(messageEvent.data, Charsets.UTF_8).trim()
                    Log.d(TAG, "📱 [MainViewModel] 휴대폰 응답 수신: $replyStatus")
                    viewModelScope.launch {
                        _mobileStatusEventBus.emit(replyStatus)

                        if (replyStatus == "READY") {
                            _mobileStatus.value = MobileServiceStatus.READY
                        } else {
                            _mobileStatus.value = MobileServiceStatus.SERVICE_RESTRICTED
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [MainViewModel] 상태 응답 패킷 파싱 실패", e)
                }
            }
            "/response_locations" -> {
                try {
                    val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                    val type = object : TypeToken<List<WatchLiveStatus>>() {}.type
                    val decryptedList: List<WatchLiveStatus> = Gson().fromJson(jsonStr, type)

                    // 수신한 데이터를 전역 싱글톤 저장소에 업데이트합니다.
                    WatchDataRepository.updateLocationList(decryptedList)
                    Log.d(TAG, "🎯 [전역 저장소 저장 완료] 친구 위치 인원: ${decryptedList.size}명")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 위치 데이터 패킷 파싱 실패", e)
                }
            }
            // 휴대폰에서 전송한 발자취 데이터 수신 처리
            "/response_histories" -> {
                try {
                    val jsonStr = String(messageEvent.data, Charsets.UTF_8)

                    // 전역 싱글톤 레포지토리에 히스토리 데이터를 넘김
                    WatchDataRepository.updateHistoriesFromJson(jsonStr)
                    Log.d(TAG, "👣 [전역 저장소 저장 완료] 발자취(History) 데이터 파싱 성공")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 발자취(History) 데이터 패킷 파싱 실패", e)
                }
            }
        }
    }

    // 메모리 누수 방지를 위한 리스너 해제
    override fun onCleared() {
        super.onCleared()
        Wearable.getMessageClient(getApplication()).removeListener(this)
    }

    /**
     * 모바일 기기로 워치 화면 활성화 상태 전송
     */
    suspend fun sendWatchStateToPhone(isActive: Boolean) {
        try {
            val messageClient = Wearable.getMessageClient(application)
            val path = "/watch_state"
            val payload = isActive.toString().toByteArray()
            val nodes = Wearable.getNodeClient(application).connectedNodes.await()

            for (node in nodes) {
                messageClient.sendMessage(node.id, path, payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "상태 전송 중 실패", e)
        }
    }

    /**
     * 수신 음성 메시지 처리 및 재생
     */
    private fun handleIncomingVoiceMessage(messageEvent: MessageEvent) {
        try {
            val payload = String(messageEvent.data, Charsets.UTF_8)
            val data = Gson().fromJson(payload, Map::class.java)
            val messageId = data["messageId"] as String
            val voiceUrl = data["voiceUrl"] as String
            val senderName = data["senderName"] as String
            val senderProfileImage = data["senderProfileImage"] as String

            val voiceData = WatchVoiceData(messageId, voiceUrl, senderProfileImage, senderName)

            if (isWatchActiveInForeground) {
                // 워치 포그라운드 → 바로 재생
                viewModelScope.launch(Dispatchers.Main) {
                    VoiceEventBus.emitVoice(voiceData)
                }
            } else {
                // 워치 백그라운드 → 알림 띄우기
                WatchNotificationHelper.showWalkieNotification(getApplication(), voiceData)
            }

            Log.d(TAG, "📥 [중앙 서비스] 무전 패킷 UI 버스로 전달 완료. 서비스 바인딩 해제 허용.")
        } catch (e: Exception) {
            Log.e(TAG, "무전 패킷 처리 중 에러", e)
        }
    }

    fun handlePlayIntent(intent: Intent) {
        val autoPlay = intent.getBooleanExtra("auto_play", false)
        if (!autoPlay) return

        val voiceUrl = intent.getStringExtra("voice_url") ?: return
        val messageId = intent.getStringExtra("message_id") ?: return
        val senderName = intent.getStringExtra("sender_name") ?: ""
        val senderProfileImage = intent.getStringExtra("sender_profile_image") ?: ""

        Log.d(TAG, "⌚ auto_play 감지 → 즉시 재생: messageId=$messageId")

        val voiceData = WatchVoiceData(
            messageId = messageId,
            voiceUrl = voiceUrl,
            senderProfileUrl = senderProfileImage,
            senderName = senderName
        )

        viewModelScope.launch {
            VoiceEventBus.emitVoice(voiceData)
        }
    }
}
