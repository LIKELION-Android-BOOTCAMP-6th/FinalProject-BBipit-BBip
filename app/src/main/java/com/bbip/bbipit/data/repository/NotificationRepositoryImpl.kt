// data/repository/NotiRepositoryImpl.kt
package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSource
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val dataSource: NotificationRemoteDataSource,
    private val firebaseFunctions: FirebaseFunctions
) : NotificationRepository {

    override suspend fun getNotiList(userId: String): Result<List<Notification>> {
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
        val data = hashMapOf(
            "type" to type,
            "notificationId" to notificationId
        )
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("markNotificationsAsRead")
                .call(data)
                .await()
            val res = result.data as? Map<*, *>
            Result.success(res?.get("success") as? Boolean ?: false)
        } catch (e: Exception) {
            Log.e("NotiRepository", "알림 읽음 처리 실패: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteNotifications(type: String, notificationId: String?): Result<Boolean> {
        val data = hashMapOf(
            "type" to type,
            "notificationId" to notificationId
        )
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("deleteNotifications")
                .call(data)
                .await()
            val res = result.data as? Map<*, *>
            Result.success(res?.get("success") as? Boolean ?: false)
        } catch (e: Exception) {
            Log.e("NotiRepository", "알림 삭제 실패: ${e.message}")
            Result.failure(e)
        }
    }
}

    /* override fun observeNewNoti(
        userId: String,
        onNew: (Notifications) -> Unit
    ): ListenerRegistration {
        return dataSource.observeNewNotification(userId) { id, dto ->
            onNew(dto.toEntity(id))
        }
    }
}*/