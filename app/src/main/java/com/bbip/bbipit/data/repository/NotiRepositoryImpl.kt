package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.remote.noti.NotiRemoteDataSource
import com.bbip.bbipit.domain.entity.NotiItem
import com.bbip.bbipit.domain.repository.NotiRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotiRepositoryImpl @Inject constructor(
    private val dataSource: NotiRemoteDataSource,
    private val firebaseFunctions: FirebaseFunctions
) : NotiRepository {

    override suspend fun getNotiList(userId: String): Result<List<NotiItem>> {
        return try {
            val response = dataSource.fetchNotifications(userId)
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