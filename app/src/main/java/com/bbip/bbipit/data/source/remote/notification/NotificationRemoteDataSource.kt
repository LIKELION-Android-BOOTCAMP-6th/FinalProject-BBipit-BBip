package com.bbip.bbipit.data.source.remote.notification

import com.bbip.bbipit.data.source.model.NotificationDto
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // 기존 알림 목록 단건 조회
    suspend fun fetchNotification(userId: String): List<Pair<String, NotificationDto>> {
        return firestore
            .collection("Users")
            .document(userId)
            .collection("Notifications")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val dto = doc.toObject(NotificationDto::class.java) ?: return@mapNotNull null
                Pair(doc.id, dto)
            }
    }

    // 실시간 구독 — 새 알림 감지
    fun observeNewNotification(
        userId: String,
        onNew: (String, NotificationDto) -> Unit
    ): ListenerRegistration {
        return firestore
            .collection("Notifications")  // 1. 루트 컬렉션을 Notifications로 변경
            .document(userId)             // 2. 문서 ID는 사용자 userId (receiver_id 위치)
            .collection("Notification")   // 3. 서브 컬렉션 이름은 단수형인 'Notification'
            .whereEqualTo("is_read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // 에러 로그 기록 필요 시 처리
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    // 새로 추가된(수신된) 미읽음 알림만 처리
                    if (change.type == DocumentChange.Type.ADDED) {
                        val dto = change.document.toObject(NotificationDto::class.java)
                        onNew(change.document.id, dto)
                    }
                }
            }
    }

    // Firestore에서 알림 삭제
    suspend fun deleteNotification(userId: String, notiId: String?) {
        if (notiId != null) {
            firestore
                .collection("Users")
                .document(userId)
                .collection("Notifications")
                .document(notiId)
                .delete()
                .await()
        }
    }
}