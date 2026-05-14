package com.bbip.bbipit.data.source.remote.noti

import com.bbip.bbipit.data.source.model.NotificationDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotiRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun fetchNotifications(userId: String): List<Pair<String, NotificationDto>> {
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
}