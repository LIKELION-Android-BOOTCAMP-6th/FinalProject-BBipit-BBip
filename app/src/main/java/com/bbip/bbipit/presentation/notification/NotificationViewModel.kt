package com.bbip.bbipit.presentation.notification

import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.bbip.bbipit.domain.usecase.GetNotificationListUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val getNotificationListUseCase: GetNotificationListUseCase,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notification = MutableStateFlow<List<Notification>>(emptyList())
    val notification = _notification.asStateFlow()

    // 전체 확인 버튼 클릭 여부 (보라색 점 제거용, isRead와 무관)
    private val _readAllClicked = MutableStateFlow(false)
    val readAllClicked = _readAllClicked.asStateFlow()

    // 현재 사용자의 UID를 가져오되, 로그인이 안 된 경우를 대비해 null 허용으로 체크
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private var notiListener: ListenerRegistration? = null

    init {
            // 1. 빈 화면이 안 나오게 로컬 더미 데이터를 먼저
            val now = System.currentTimeMillis()
            _notification.value = listOf(
                Notification(
                    notificationId = "1",
                    type = "WALKIE",
                    senderName = "JEON",
                    content = "반갑습니다!",
                    createdAt = now,
                    isRead = false,
                    expiresAt = now + 10800000L
                ),
                Notification(
                    notificationId = "2",
                    type = "DM",
                    senderName = "KIM",
                    content = "데이터 연동 확인용 알림입니다.",
                    createdAt = now - 600000L,
                    isRead = false,
                    expiresAt = System.currentTimeMillis() + 3 * 60 * 60 * 1000L),
                Notification(
                    notificationId = "3",
                    type = "REQ",
                    senderName = "JANG",
                    content = "친구 요청",
                    createdAt = now - 600000L,
                    isRead = false,
                    expiresAt = System.currentTimeMillis() + 3 * 60 * 60 * 1000L),
                Notification(
                    notificationId = "4",
                    type = "WALKIE",
                    senderName = "LEE",
                    content = "반갑습니다!",
                    createdAt = now,
                    isRead = false,
                    expiresAt = now + 10800000L
                ),
                Notification(
                    notificationId = "5",
                    type = "DM",
                    senderName = "HYEON",
                    content = "데이터 연동 확인용 알림입니다.",
                    createdAt = now - 600000L,
                    isRead = false,
                    expiresAt = System.currentTimeMillis() + 3 * 60 * 60 * 1000L),
                Notification(
                    notificationId = "6",
                    type = "REQ",
                    senderName = "VIC",
                    content = "친구 요청",
                    createdAt = now - 600000L,
                    isRead = false,
                    expiresAt = System.currentTimeMillis() + 3 * 60 * 60 * 1000L)
            )


            // 2. 실제 Firebase 데이터를 가져오려고 시도
            if (currentUserId.isNotEmpty()) {
                fetchNotifications()
                observeIncomingNoti()
            }
        }


    fun fetchNotifications() {
        viewModelScope.launch {
            _readAllClicked.value = false
            getNotificationListUseCase(currentUserId)
                .onSuccess { _notification.value = it }
                .onFailure { /* 추후 에러 처리 */ }
        }
    }

    // 실시간 구독 — 새 알림 수신 시 목록 상단에 추가
    private fun observeIncomingNoti() {
        notiListener = notificationRepository.observeNewNotification(currentUserId) { newNoti ->
            val exists = _notification.value.any { it.notificationId == newNoti.notificationId }
            if (!exists) {
                _notification.value = listOf(newNoti) + _notification.value
            }
        }
    }

    // 리스트에서 완전히 삭제 (스와이프 시) + Firestore 반영
    fun markAsReadAndDelete(notiId: String) {
        // 로컬 즉시 반영 (UX 끊김 없게)
        _notification.value = _notification.value.filter { it.notificationId != notiId }

        // Firestore 비동기 삭제
        viewModelScope.launch {
            notificationRepository.deleteNotifications(currentUserId, notiId)
                .onFailure { fetchNotifications() }  // 실패 시 원복
        }
    }

    // 단순 읽음 처리 (DM 클릭, 무전 클릭 시)
    fun markAsRead(notiId: String) {
        _notification.value = _notification.value.map {
            if (it.notificationId == notiId) it.copy(isRead = true) else it
        }
    }

    // 친구 수락 클릭 (isRead = true + 배지용 content 변경)
    fun onAcceptFriendClick(notiId: String) {
        _notification.value = _notification.value.map {
            if (it.notificationId == notiId) it.copy(isRead = true, content = "수락됨") else it
        }
    }

    // 친구 거절 클릭 (isRead = true + 배지용 content 변경)
    fun onRejectFriendClick(notiId: String) {
        _notification.value = _notification.value.map {
            if (it.notificationId == notiId) it.copy(isRead = true, content = "거절됨") else it
        }
    }

    // 전체 확인: 보라색 점만 제거 (isRead는 건드리지 않음)
    fun onReadAllClick() {
        _readAllClicked.value = true
    }

    // ViewModel 소멸 시 리스너 해제
    override fun onCleared() {
        super.onCleared()
        notiListener?.remove()
    }

    // 테스트용 함수 추가
    fun insertTestData() {
        viewModelScope.launch {
            val now = com.google.firebase.Timestamp.now()
            val testData = listOf(
                mapOf(
                    "type" to "DM",
                    "sender_id" to "test_uid_1",
                    "sender_name" to "테스트유저",
                    "content" to "안녕하세요!",
                    "voice_url" to "",
                    "room_id" to "room_test",
                    "is_read" to false,
                    "created_at" to now,
                    "expires_at" to null
                ),
                mapOf(
                    "type" to "WALKIE",
                    "sender_id" to "test_uid_2",
                    "sender_name" to "Alex Rivera",
                    "content" to "",
                    "voice_url" to "",
                    "room_id" to "",
                    "is_read" to false,
                    "created_at" to now,
                    "expires_at" to null
                ),
                mapOf(
                    "type" to "REQ",
                    "sender_id" to "test_uid_3",
                    "sender_name" to "Jordan",
                    "content" to "",
                    "voice_url" to "",
                    "room_id" to "",
                    "is_read" to false,
                    "created_at" to now,
                    "expires_at" to null
                )
            )

            testData.forEach { data ->
                FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(currentUserId)
                    .collection("Notifications")
                    .add(data)
            }

            // 삽입 후 바로 불러오기
            fetchNotifications()
        }
    }
    fun addDummyNoti() {
        val now = System.currentTimeMillis()
        val newNoti = Notification(
            notificationId = "dummy_${now}", // 중복 방지를 위해 현재 시간 사용
            type = "WALKIE",
            senderName = "테스트 유저",
            content = "방금 생성된 테스트 알림입니다!",
            createdAt = now,
            isRead = false,
            expiresAt = now + 3 * 60 * 60 * 1000L
        )
        // 현재 리스트 맨 앞에 추가
        _notification.value = listOf(newNoti) + _notification.value
    }
}

