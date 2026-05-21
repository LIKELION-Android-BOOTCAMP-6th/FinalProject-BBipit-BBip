package com.bbip.bbipit.presentation.notification

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.R
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notification = MutableStateFlow<List<Notification>>(emptyList())
    val notification: StateFlow<List<Notification>> = _notification.asStateFlow()

    private val _expiredVoiceIds = MutableStateFlow<Set<String>>(emptySet())
    val expiredVoiceIds: StateFlow<Set<String>> = _expiredVoiceIds.asStateFlow()

    private val _readAllClicked = MutableStateFlow(false)
    val readAllClicked: StateFlow<Boolean> = _readAllClicked.asStateFlow()

    private val currentUserId: String = authRepository.getCurrentUserUid() ?: ""

    private val _readIds = MutableStateFlow<Set<String>>(emptySet())
    val readIds: StateFlow<Set<String>> = _readIds.asStateFlow()

    private val _showInAppBanner = MutableStateFlow(false)
    val showInAppBanner: StateFlow<Boolean> = _showInAppBanner.asStateFlow()

    private val _latestInAppNotification = MutableStateFlow<Notification?>(null)
    val latestInAppNotification: StateFlow<Notification?> = _latestInAppNotification.asStateFlow()

    private val _deletedIds = MutableStateFlow<Set<String>>(emptySet())


    private var isObserving = false

    init {
        Log.d("NotificationVM", "ViewModel init 호출")
        if (currentUserId.isNotEmpty() && !isObserving) {
            isObserving = true
            if (isNetworkAvailable()) {
                observeIncomingNotification()
            }
        }
    }

    private var isInitialLoad = true

    private fun observeIncomingNotification() {
        viewModelScope.launch {
            try {
                notificationRepository.observeNotification(currentUserId)
                    .collect { liveNotifications ->

                        val safeNotifications = liveNotifications.filter {
                            it.id !in _deletedIds.value
                        }

                        if (!isInitialLoad) {
                            val newlyAdded = safeNotifications.filter { new ->
                                _notification.value.none { old -> old.id == new.id }
                            }

                            if (newlyAdded.isNotEmpty()) {
                                val brandNew = newlyAdded.maxByOrNull { it.createdAt }
                                if (brandNew != null && !brandNew.isRead) {
                                    Log.d("NotificationVM", "배너 트리거: ${brandNew.type}, id: ${brandNew.id}")
                                    triggerInAppBanner(brandNew)
                                }
                            }
                        } else {
                            isInitialLoad = false
                            Log.d("NotificationVM", "초기 로드 완료 — 이후 새 알림부터 배너 표시")
                        }

                        Log.d("NotificationVM", "전체: ${liveNotifications.size}, 필터 후: ${safeNotifications.size}")
                        _notification.value = safeNotifications
                    }
            } catch (e: Exception) {
                Log.e("NotificationVM", "알림 스트림 수신 에러: ${e.message}")
            }
        }
    }

    private fun triggerInAppBanner(noti: Notification) {
        viewModelScope.launch {
            _latestInAppNotification.value = noti
            _showInAppBanner.value = true

            showSystemNotification(noti)
        }
    }

    private fun showSystemNotification(notification: Notification) {
        // 1. 알림 채널(Channel) 설정 (Android 8.0 이상 필수)
        val channelId = "phone_alert_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "중요 알림"
            val descriptionText = "앱의 중요 안내를 전달합니다."

            // 스마트폰 화면 위로 팝업 배너(Heads-up)가 노출되도록 중요도를 HIGH로 설정
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            // 시스템에 채널 등록
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 본문 분기 처리 (REQ일 때와 일반 내용일 때)
        val bodyText = when (notification.type) {
            "REQ" -> "친구 요청이 왔습니다."
            else -> notification.content
        }

        // 2. NotificationCompat.Builder를 통한 알림 생성 (주입받은 context 활용)
        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.baseline_notifications_24) // 방금 생성에 성공한 순정 아이콘 리소스 적용
            .setContentTitle(notification.senderName)
            .setContentText(bodyText)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH) // 상단 팝업 배너 필수 우선순위
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)   // 진동 및 소리 활성화로 배너 노출 보장
            .setAutoCancel(true)

        // 3. 알림 발생시키기
        try {
            with(androidx.core.app.NotificationManagerCompat.from(context)) {
                notify(notification.id.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationVM", "알림 권한이 거부되어 팝업을 띄울 수 없습니다: ${e.message}")
        }
    }

    fun dismissBanner() {
        _showInAppBanner.value = false
    }

    // 현재 기기의 네트워크 연결 상태를 체크하는 함수
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun showNetworkErrorToast() {
        Toast.makeText(
            context,
            "네트워크 연결이 원활하지 않습니다. 연결 상태를 확인해주세요.",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun setVoiceExpired(id: String) {
        _expiredVoiceIds.value += id
    }

    // 리스트에서 완전히 삭제 (스와이프 시) + Cloud Functions 연동
    fun markAsReadAndDelete(id: String) {
        Log.d("NotificationVM", "삭제 요청 id: $id")

        _deletedIds.value += id

        // 로컬 즉시 반영
        _notification.value = _notification.value.filter { it.id != id }

        viewModelScope.launch {
            try {
                notificationRepository.deleteNotifications(currentUserId, id)
            } catch (e: Exception) {
                Log.e("NotificationVM", "서버 삭제 실패: ${e.message}")
                // 실패 시 deletedIds에서 제거
                _deletedIds.value -= id
            }
        }
    }

    fun markAsRead(id: String) {
        if (currentUserId.isEmpty()) return
        if (!isNetworkAvailable()) { showNetworkErrorToast(); return }

        _readIds.value += id

        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("Notifications")
                    .document(currentUserId)
                    .collection("Notification")
                    .document(id)
                    .update("is_read", true)
                    .addOnSuccessListener {
                        Log.d("NotificationVM", "Firestore 직접 업데이트 성공: $id")
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotificationVM", "Firestore 직접 업데이트 실패: ${e.message}")
                        _readIds.value -= id
                    }
            } catch (e: Exception) {
                Log.e("NotificationVM", "코루틴 에러: ${e.message}")
                _readIds.value -= id
            }
        }
    }

    // 전체 확인: 보라색 점만 제거 (isRead는 건드리지 않음)
    fun onReadAllClick() {
        _readAllClicked.value = true

        viewModelScope.launch {
            notificationRepository.markNotificationsAsRead("all", null)
        }
    }


    fun createTestNotification(type: String) {
        val userId = authRepository.getCurrentUserUid() ?: ""
        if (userId.isEmpty()) return

        val generatedId = FirebaseFirestore.getInstance()
            .collection("Notifications")
            .document(userId)
            .collection("Notification")
            .document().id

        // 🚨 [진짜 해결]: java.util.Date()를 생성한 뒤, 파이어베이스 Timestamp 객체로 변환합니다.
        // 이렇게 해야 엔티티 모델인 Timestamp 타입과 완벽하게 매칭되어 크래시가 나지 않습니다.
        val firebaseTimestamp = com.google.firebase.Timestamp(java.util.Date())

        val testData = hashMapOf(
            "id" to generatedId,
            "type" to type,
            "senderName" to when(type) {
                "DM" -> "홍길동(DM)"
                "WALKIE" -> "김철수(무전)"
                else -> "이영희(친구요청)"
            },
            "content" to when(type) {
                "DM" -> "지금 뭐해? 메시지 보냄!"
                "WALKIE" -> "치익- 무전을 보냈습니다."
                else -> "친구 요청을 보냈습니다."
            },
            "is_read" to false,
            // 🚨 숫자가 아닌 파이어베이스 순정 Timestamp 객체를 삽입합니다.
            "created_at" to com.google.firebase.Timestamp.now(),            "roomId" to if (type == "DM") "test_room_123" else "",
            "isExpired" to false,
            "expiresAt" to if (type == "WALKIE") System.currentTimeMillis() + (3 * 60 * 60 * 1000L) else 0L
        )

        FirebaseFirestore.getInstance()
            .collection("Notifications")
            .document(userId)
            .collection("Notification")
            .document(generatedId)
            .set(testData)
            .addOnSuccessListener {
                Log.d("NotificationVM", "🚀 테스트 알림 ($type) 생성 성공!")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationVM", "❌ 테스트 알림 생성 실패: ${e.message}")
            }
    }
}