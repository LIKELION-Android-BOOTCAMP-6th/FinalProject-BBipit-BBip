package com.bbip.bbipit.data.source.remote.notification

import com.bbip.bbipit.data.source.model.NotificationDto
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 관련 원격 데이터 소스 구현체입니다.
 * Firestore를 통해 알림 데이터의 조회, 삭제 및 실시간 구독을 처리합니다.
 */
@Singleton
class NotificationRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRemoteDataSource {

    // 알림 목록 조회
    override suspend fun fetchNotification(userId: String): List<Pair<String, NotificationDto>> {
        return firestore
            .collection("Notifications")
            .document(userId)
            .collection("Notification")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val dto = doc.toObject(NotificationDto::class.java) ?: return@mapNotNull null
                Pair(doc.id, dto)
            }
    }

    // 새 알림 실시간 구독
    override fun observeNewNotification(
        userId: String,
        onNew: (String, NotificationDto) -> Unit
    ): ListenerRegistration {
        return firestore
            .collection("Notifications")
            .document(userId)
            .collection("Notification")
            .whereEqualTo("is_read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val dto = change.document.toObject(NotificationDto::class.java) ?: return@forEach
                        onNew(change.document.id, dto)
                    }
                }
            }
    }

    // 알림 삭제
    override suspend fun deleteNotification(userId: String, notiId: String?) {
        if (notiId != null) {
            firestore
                .collection("Notifications")
                .document(userId)
                .collection("Notification")
                .document(notiId)
                .delete()
                .await()
        }
    }
}