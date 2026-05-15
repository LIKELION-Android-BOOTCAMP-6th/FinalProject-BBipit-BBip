// data/repository/NotiRepositoryImpl.kt
package com.bbip.bbipit.data.repository

import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSource
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val dataSource: NotificationRemoteDataSource
) : NotificationRepository {

    override suspend fun getNotiList(userId: String): Result<List<Notification>> {
        return try {
            val response = dataSource.fetchNotification(userId)
            Result.success(response.map { (id, dto) -> dto.toEntity(id) })
        } catch (e: Exception) {
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