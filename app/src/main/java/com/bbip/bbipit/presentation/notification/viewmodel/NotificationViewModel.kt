package com.bbip.bbipit.presentation.notification.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private val _notification = MutableStateFlow<List<Notification>>(emptyList())
    val notification: StateFlow<List<Notification>> = _notification.asStateFlow()

    private val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    private val _expiredVoiceIds = MutableStateFlow<Set<String>>(
        prefs.getStringSet("expired_voice_ids", emptySet()) ?: emptySet()
    )
    val expiredVoiceIds: StateFlow<Set<String>> = _expiredVoiceIds.asStateFlow()

    private val _readAllClicked = MutableStateFlow(false)
    val readAllClicked: StateFlow<Boolean> = _readAllClicked.asStateFlow()

    private val currentUserId: String = authRepository.getCurrentUserUid() ?: ""

    init {
        viewModelScope.launch {
            notificationRepository.notifications.collect { liveNotifications ->
                if (liveNotifications.size > _notification.value.size) {
                    _readAllClicked.value = false
                }

                _notification.value = liveNotifications
                    .sortedByDescending { it.createdAt }
                    .toList()
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

    // 단건 읽음 처리
    fun markAsRead(id: String) {
        if (currentUserId.isEmpty()) return
        if (!isNetworkAvailable()) { showNetworkErrorToast(); return }

        // 로컬 UI 상태 반영
        _notification.value = _notification.value.map { notification ->
            if (notification.id == id) notification.copy(isRead = true)
            else notification
        }

        viewModelScope.launch {
            Log.d("NotificationVM", "📱 서버에 단건 읽음 요청 전송 시작: $id")
            val result = notificationRepository.markNotificationsAsRead(type = "single", notificationId = id)

            result.onSuccess {
                Log.d("NotificationVM", "✅ 서버 단건 읽음 처리 완료: $id")
            }.onFailure { e ->
                Log.e("NotificationVM", "❌ 서버 단건 읽음 처리 실패: ${e.message}")
            }
        }
    }

    // 전체 확인 처리
    fun onReadAllClick() {
        if (!isNetworkAvailable()) { showNetworkErrorToast(); return }

        // 로컬 UI 상태 즉시 전체 true 변환
        _notification.value = _notification.value.map { it.copy(isRead = true) }
        _readAllClicked.value = true

        viewModelScope.launch {
            Log.d("NotificationVM", "📱 서버에 전체 읽음 요청 전송 시작")
            val result = notificationRepository.markNotificationsAsRead(type = "all", notificationId = null)

            result.onSuccess { success ->
                if (success) {
                    Log.d("NotificationVM", "✅ 서버 전체 읽음 처리 완료 API 호출 성공")
                } else {
                    Log.e("NotificationVM", "❌ 서버 전체 읽음 처리 API 가 false를 반환함")
                }
            }.onFailure { e ->
                Log.e("NotificationVM", "❌ 서버 전체 읽음 처리 호출 완전 실패: ${e.message}")
            }
        }
    }

    fun onClickAudioNotification(notificationId: String, voiceId: String){
        viewModelScope.launch {
            // 서버에서 음성 메시지 조회
            val result = voiceRepository.getVoiceMessageById(voiceId)

            when (result) {
                is Result.Success -> {
                    notificationRepository.markVoiceNotificationAsPlayed(notificationId)

                    val voiceMessage = result.data
                    Log.d("NotificationViewModel", result.data.toString())
                    voiceRepository.emitMobileVoiceEvent(voiceMessage)
                }
                is Result.Failure -> {
                    // 필요 시 에러 토스트 팝업이나 로그 처리 추가 가능
                    Log.e("NotificationViewModel", "음성 메시지 재생 실패: ${result.error}")
                }
            }
        }
    }
    fun markDmNotificationsAsRead(roomId: String) {
        val dmNotifications = _notification.value.filter {
            it.type == "DM" && it.roomId == roomId && !it.isRead
        }
        dmNotifications.forEach { markAsRead(it.id) }
    }

    fun deleteAllNotifications() {
        if (currentUserId.isEmpty()) return
        if (!isNetworkAvailable()) { showNetworkErrorToast(); return }

        viewModelScope.launch {
            try {
                notificationRepository.deleteNotifications(currentUserId, null)
            } catch (e: Exception) {
                Log.e("NotificationVM", "전체 삭제 실패: ${e.message}")
            }
        }
    }

    // ACP 타입 읽음 처리
    fun markAcpNotificationsAsRead() {
        val acpNotifications = _notification.value.filter {
            it.type == "ACP" && !it.isRead
        }
        acpNotifications.forEach { markAsRead(it.id) }
    }
}