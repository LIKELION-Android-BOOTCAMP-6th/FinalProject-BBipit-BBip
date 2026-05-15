// data/repository/NotiRepositoryImpl.kt
package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSource
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val dataSource: NotificationRemoteDataSource
) : NotificationRepository {

    override suspend fun getNotificationList(userId: String): Result<List<Notification>> {
        return try {
            val response = dataSource.fetchNotification(userId)
            Result.success(response.map { (id, dto) -> dto.toEntity(id) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markNotificationsAsRead(
        type: String,
        notificationId: String?
    ): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun observeNewNotification(
        userId: String,
        onNew: (Notifications) -> Unit
    ): ListenerRegistration {
        return dataSource.observeNewNotification(userId) { id, dto ->
            onNew(dto.toEntity(id))
        }
    }

    override suspend fun deleteNotification(userId: String, notiId: String): Result<Unit> {
        return try {
            dataSource.deleteNotification(userId, notiId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}