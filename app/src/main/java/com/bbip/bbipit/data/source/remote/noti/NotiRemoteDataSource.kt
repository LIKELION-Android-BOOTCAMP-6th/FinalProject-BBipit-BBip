package com.bbip.bbipit.data.source.remote.noti

import com.bbip.bbipit.data.source.model.NotiDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotiRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // 기존 알림 목록 단건 조회
    suspend fun fetchNotifications(userId: String): List<Pair<String, NotiDto>> {
        return firestore
            .collection("Users")
            .document(userId)
            .collection("Notifications")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val dto = doc.toObject(NotiDto::class.java) ?: return@mapNotNull null
                Pair(doc.id, dto)
            }
    }
}