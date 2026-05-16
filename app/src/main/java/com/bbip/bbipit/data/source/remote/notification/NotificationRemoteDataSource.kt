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
            .collection("Users")
            .document(userId)
            .collection("Notifications")
            .whereEqualTo("is_read", false)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    // 새로 추가된 알림만 처리
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