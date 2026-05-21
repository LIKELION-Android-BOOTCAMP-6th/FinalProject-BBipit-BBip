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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _deletedIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            // Repository 캐시 구독 → UI 갱신만 담당
            notificationRepository.notifications.collect { liveNotifications ->
                // 데이터 정렬
                _notification.value = liveNotifications.sortedByDescending { it.createdAt }
            }
        }
    }

    // 현재 기기의 네트워크 연결 상태를 체크하는 함수
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    // 리스트에서 완전히 삭제 (스와이프 시)
    fun markAsReadAndDelete(id: String) {
        Log.d("NotificationVM", "삭제 요청 id: $id")
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotifications(currentUserId, id)
            } catch (e: Exception) {
                Log.e("NotificationVM", "서버 삭제 실패: ${e.message}")
            }
        }
    }

    fun markAsRead(id: String) {
        if (currentUserId.isEmpty()) return
        if (!isNetworkAvailable()) {
            showNetworkErrorToast(); return
        }

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

        val testData = hashMapOf(
            "id" to generatedId,
            "type" to type,
            "senderName" to when (type) {
                "DM" -> "홍길동(DM)"
                "WALKIE" -> "김철수(무전)"
                else -> "이영희(친구요청)"
            },
            "content" to when (type) {
                "DM" -> "지금 뭐해? 메시지 보냄!"
                "WALKIE" -> "치익- 무전을 보냈습니다."
                else -> "친구 요청을 보냈습니다."
            },
            "is_read" to false,
            "created_at" to Timestamp.now(),
            "roomId" to if (type == "DM") "test_room_123" else "",
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