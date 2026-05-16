// data/repository/NotiRepositoryImpl.kt
package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSource
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 알림 데이터 처리 구현체
 * RemoteDataSource 및 Firebase Functions를 활용한 알림 조회, 읽음 처리, 삭제 기능 수행
 */
class NotificationRepositoryImpl @Inject constructor(
    private val dataSource: NotificationRemoteDataSource,
    private val firebaseFunctions: FirebaseFunctions,
) : NotificationRepository {

    // 사용자별 알림 목록 조회 처리
    override suspend fun getNotificationList(userId: String): Result<List<Notification>> {
        return try {
            val response = dataSource.fetchNotification(userId)
            Result.success(response.map { (id, dto) -> dto.toEntity(id) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 알림 읽음 상태 업데이트 요청 처리
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

    override fun observeNewNotification(
        userId: String,
        onNew: (Notification) -> Unit
    ): ListenerRegistration {
        return dataSource.observeNewNotification(userId) { id, dto ->
            onNew(dto.toEntity(id))
        }
    }

    // 알림 삭제 요청 처리
    override suspend fun deleteNotifications(userId: String, notiId: String?): Result<Unit> {
        return try {
            dataSource.deleteNotification(userId, notiId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
