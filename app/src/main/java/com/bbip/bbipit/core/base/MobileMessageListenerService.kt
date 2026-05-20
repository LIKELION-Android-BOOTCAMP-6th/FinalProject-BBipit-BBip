package com.bbip.bbipit.core.base

import com.google.android.gms.common.api.ApiException
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 페어링 스마트워치 송신 저수준 데이터 패킷 메시지 상시 리스닝 및 처리용 리스너 서비스 클래스
 */
@AndroidEntryPoint
class MobileMessageListenerService : WearableListenerService() {
    @Inject
    lateinit var liveStatusRepository: LiveStatusRepository
    @Inject
    lateinit var voiceRepository: VoiceRepository

    // 하위 스레드 단위 비동기 리포지토리 작업 통제용 입출력(IO) 전용 코루틴 스코프
    private val scope = CoroutineScope(Dispatchers.IO)
    private val TAG = "MobileMessageService"

    /**
     * 서비스 라이프사이클 종료 시 가동 중인 백그라운드 코루틴 프로세스 안전 소멸 콜백 함수
     */
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    /**
     * 워치 송신 메시지 스마트폰 도달 시점 자동 분기 호출 이벤트 핸들러 함수
     * 인입 고유 경로 식별자(Path) 해석 기반 내부 전용 핸들러 함수 연결 처리 위임 목적
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("MobileMessageService", "메시지 수신됨. Path: ${messageEvent.path}")

        when (messageEvent.path) {
            "/check_phone_status" -> {
                handleMobileStatusCheck(messageEvent.sourceNodeId)
            }
            "/request_friends_location" -> {
                handleFriendsLocationRequest(messageEvent.sourceNodeId)
            }
            "/mark_voice_read" -> {
                val messageId = String(messageEvent.data, Charsets.UTF_8)
                handleMarkVoiceRead(messageId)
            }
        }
    }

    /**
     * 워치 측 음성 데이터 수신 및 청취 완료 신호 수신 기반 서버 저장소 내 읽음 확정 마크 처리 전용 핸들러 함수
     */
    private fun handleMarkVoiceRead(messageId: String) {
        scope.launch {
            try {
                voiceRepository.markVoiceMessageAsRead(messageId)
                    .onSuccess { Log.d(TAG, "음성 메시지 읽음 처리 성공: $messageId") }
                    .onFailure { Log.e(TAG, "음성 메시지 읽음 처리 실패: $messageId, error: $it") }
            } catch (e: Exception) {
                Log.e(TAG, "음성 메시지 읽음 처리 중 예외 발생: $messageId", e)
            }
        }
    }

    /**
     * 워치 지리 지도 화면 갱신용 최신 위치 리스트 수동 요청 대응 핸들러 함수
     * 인메모리 싱글톤 캐시 및 현재 플로우 계류 주변 사용자 좌표 리스트 병합 직렬화 반환 목적
     */
    private fun handleFriendsLocationRequest(senderNodeId: String) {
        scope.launch {
            try {
                val myLocation = liveStatusRepository.getCachedMyLiveStatus()
                val friendsLocations = liveStatusRepository.friendsLiveStatusFlow.value

                val totalLocationsList = mutableListOf<LiveStatus>()
                myLocation?.let { totalLocationsList.add(it) }
                friendsLocations.let { totalLocationsList.addAll(it) }

                val jsonPayload = Gson().toJson(totalLocationsList)
                val byteArray = jsonPayload.toByteArray(Charsets.UTF_8)

                // 병합 생성 바이너리 JSON 패킷의 발신 워치 노드 아이디 주소 대상 즉시 전송 처리
                Wearable.getMessageClient(this@MobileMessageListenerService)
                    .sendMessage(senderNodeId, "/response_friends_location", byteArray)
                    .await()
                Log.d(TAG, "✅ [WearableService] 워치 단말로 위치 패킷 전송 성공! (총 ${totalLocationsList.size}명)")
            } catch (e: ApiException) {
                if (e.statusCode == 17) { // 17: CommonStatusCodes.API_UNAVAILABLE
                    Log.d(TAG, "워치 API 미지원 기기이므로 워치 연동 작업 생략")
                } else {
                    Log.e(TAG, "❌ 워치 통신 에러", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [WearableService] 워치로 위치 데이터 전송 실패", e)
            }
        }
    }

    /**
     * 워치 앱 초기화 프로세스 중 통신 필수 하드웨어 권한 수락 여부 검증 및 상태 회신 함수
     * 안드로이드 12(S, API 31) 이상 버전 기준 근거리 블루투스 감지 권한 소유 유무 판별 목적
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
            try {
                val messageClient = Wearable.getMessageClient(this@MobileMessageListenerService)
                val status = if (hasPermission) "READY" else "NEED_PERMISSION"

                // 준비 상태 또는 권한 필요 문자열 바이트 규격 부호화 기반 원격 워치 기기 피드백 처리
                messageClient.sendMessage(
                    senderNodeId,
                    "/phone_status_reply",
                    status.toByteArray(Charsets.UTF_8)
                ).await()
                Log.d("MobileMessageService", "워치에 $status 응답 전송 완료")
            } catch (e: Exception) {
                Log.e("MobileMessageService", "워치로 상태 응답 전송 실패", e)
            }
        }
    }
}