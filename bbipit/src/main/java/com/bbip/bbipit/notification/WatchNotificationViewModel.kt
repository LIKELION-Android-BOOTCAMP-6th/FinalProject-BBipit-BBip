package com.bbip.bbipit.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WatchNotificationViewModel : ViewModel() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _notification = MutableStateFlow<List<Notification>>(emptyList())
    val notification: StateFlow<List<Notification>> = _notification.asStateFlow()

    // 실시간 신규 알림 스트림 추가
    private val _bannerEvent = MutableSharedFlow<Notification>()
    val bannerEvent: SharedFlow<Notification> = _bannerEvent.asSharedFlow()

    private var currentUserId: String = "Wy102dzyw4buC0V6YJuqxjtf6qA2"
    private var currentViewingRoomId: String? = null
    private var isObserving = false
    private var isFirstSnapshot = true

    fun updateCurrentRoom(roomId: String?) {
        currentViewingRoomId = roomId
        currentViewingRoomId?.let { room ->
            _notification.value = _notification.value.filter { it.roomId != room }
        }
    }

    fun setUserId(uid: String) {
        currentUserId = uid
    }

    fun startObserving() {
        if (isObserving) return
        if (currentUserId.isEmpty()) return
        isObserving = true

        db.collection("Notifications")
            .document(currentUserId)
            .collection("Notification")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                // 전체 데이터 파싱 및 전체 리스트 업데이트
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val senderNameVal = doc.getString("sender_name") ?: doc.getString("senderName") ?: ""
                        val typeVal = doc.getString("type") ?: ""
                        val contentVal = doc.getString("content") ?: ""
                        val roomIdVal = doc.getString("room_id") ?: doc.getString("roomId")
                        val isReadVal = doc.getBoolean("is_read") ?: doc.getBoolean("isRead") ?: false
                        val timestamp = doc.getTimestamp("created_at") ?: doc.getTimestamp("createdAt")
                        val createdAtVal = timestamp?.toDate()?.time ?: 0L

                        Notification(
                            id = doc.id, senderName = senderNameVal, type = typeVal,
                            content = contentVal, createdAt = createdAtVal, roomId = roomIdVal, isRead = isReadVal
                        )
                    } catch (e: Exception) { null }
                }

                val filteredList = list.filter {
                    !(it.type == "DM" && it.roomId == currentViewingRoomId)
                }
                _notification.value = filteredList

                // 최초 진입 시 기존 데이터 배너 노출 생략 처리
                if (isFirstSnapshot) {
                    isFirstSnapshot = false
                } else {
                    // 실시간 추가 데이터 감지
                    snapshot.documentChanges.forEach { change ->
                        // 신규 추가 문서 정보 파싱 후 배너 스트림 전달
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val senderNameVal = doc.getString("sender_name") ?: doc.getString("senderName") ?: ""
                            val typeVal = doc.getString("type") ?: ""
                            val contentVal = doc.getString("content") ?: ""
                            val roomIdVal = doc.getString("room_id") ?: doc.getString("roomId")
                            val isReadVal = doc.getBoolean("is_read") ?: doc.getBoolean("isRead") ?: false
                            val timestamp = doc.getTimestamp("created_at") ?: doc.getTimestamp("createdAt")
                            val createdAtVal = timestamp?.toDate()?.time ?: 0L

                            val newNotification = Notification(
                                id = doc.id, senderName = senderNameVal, type = typeVal,
                                content = contentVal, createdAt = createdAtVal, roomId = roomIdVal, isRead = isReadVal
                            )

                            if (!(newNotification.type == "DM" && newNotification.roomId == currentViewingRoomId) && !newNotification.isRead) {
                                MainScope().launch {
                                    _bannerEvent.emit(newNotification)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun markAsRead(id: String) {
        db.collection("Notifications")
            .document(currentUserId)
            .collection("Notification")
            .document(id)
            .update("isRead", true)
    }

    // 기존 데이터 삭제 기능 유지
    fun deleteNotification(id: String) {
        db.collection("Notifications")
            .document(currentUserId)
            .collection("Notification")
            .document(id).delete()
    }
}