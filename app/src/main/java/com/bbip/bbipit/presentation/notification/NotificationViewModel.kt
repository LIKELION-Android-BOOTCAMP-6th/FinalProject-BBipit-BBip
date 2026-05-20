package com.bbip.bbipit.presentation.notification

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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


    init {
        if (currentUserId.isNotEmpty()) {
            if (isNetworkAvailable()) {
                observeIncomingNotification()
            } else {
                showNetworkErrorToast()
            }
        } else {
            Log.e("NotificationVM", "예외: currentUserId가 비어있어 구독을 차단합니다.")
        }
    }

    private fun observeIncomingNotification() {
        viewModelScope.launch {
            try {
                notificationRepository.observeNotification(currentUserId)
                    .collectLatest { liveNotifications ->
                        if (_notification.value.isNotEmpty() && liveNotifications.size > _notification.value.size) {

                            val newlyAddedNotifications = liveNotifications.filter { newNotification ->
                                _notification.value.none { oldNotification -> oldNotification.id == newNotification.id }
                            }

                            Log.d("NotificationVM", "새 알림 감지: ${newlyAddedNotifications.size}개")
                            newlyAddedNotifications.forEach {
                                Log.d("NotificationVM", "type=${it.type}, isRead=${it.isRead}, createdAt=${it.createdAt}")
                            }

                            val brandNewNotification = newlyAddedNotifications.maxByOrNull { it.createdAt }
                            if (brandNewNotification != null && !brandNewNotification.isRead) {
                                Log.d("NotificationVM", "배너 트리거: ${brandNewNotification.type}")
                                triggerInAppBanner(brandNewNotification)
                            } else {
                                Log.d("NotificationVM", "배너 트리거 안 됨: brandNew=${brandNewNotification?.type}, isRead=${brandNewNotification?.isRead}")
                            }
                        }
                        // 현재 상태 업데이트
                        _notification.value = liveNotifications.filter { it.id !in _deletedIds.value }
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
        _deletedIds.value += id
        _notification.value = _notification.value.filter { it.id != id }

        viewModelScope.launch {
            notificationRepository.deleteNotifications(currentUserId, id)
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
}