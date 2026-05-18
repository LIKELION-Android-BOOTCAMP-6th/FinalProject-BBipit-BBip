package com.bbip.bbipit.data.source.remote.notification

import com.bbip.bbipit.data.source.model.NotificationDto
import com.google.firebase.firestore.ListenerRegistration

interface NotificationRemoteDataSource {
    suspend fun fetchNotification(userId: String): List<Pair<String, NotificationDto>>

    fun observeNotification(
        userId: String,
        onNew: (String, NotificationDto) -> Unit
    ): ListenerRegistration

    suspend fun deleteNotification(userId: String, id: String?)
}