package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSource
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 관련 데이터 처리를 담당하는 구현체입니다.
 * 알림 목록 조회, 읽음 처리, 실시간 관찰 기능을 제공합니다.
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dataSource: NotificationRemoteDataSource,
    private val firebaseFunctions: FirebaseFunctions,
) : NotificationRepository {

    // 알림 목록 조회
    override suspend fun getNotificationList(userId: String): Result<List<Notification>> {
        return try {
            val response = dataSource.fetchNotification(userId)
            Result.Success(response.map { (id, dto) -> dto.toEntity(id) })
        } catch (e: Exception) {
            Log.e("NotiRepository", "알림 목록 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알림 목록을 가져오지 못했습니다."))
        }
    }

    // 알림 읽음 처리
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
            Result.Success(res?.get("success") as? Boolean ?: false)
        } catch (e: Exception) {
            Log.e("NotiRepository", "알림 읽음 처리 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알림 읽음 처리 중 오류 발생"))
        }
    }

    // 실시간 구독(Listener) 관찰용 함수는 다른 도메인의 Flow 스트림 규칙과 유사하게 
    // Result로 감싸지 않고 통과시키는 기존 형태를 유지합니다.
    // 새 알림 실시간 구독
    override fun observeNewNotification(
        userId: String,
        onNew: (Notification) -> Unit
    ): ListenerRegistration {
        return dataSource.observeNewNotification(userId) { id, dto ->
            onNew(dto.toEntity(id))
        }
    }

    // 알림 삭제
    override suspend fun deleteNotifications(userId: String, notiId: String?): Result<Unit> {
        return try {
            dataSource.deleteNotification(userId, notiId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("NotiRepository", "알림 삭제 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알림 삭제 실패"))
        }
    }
}