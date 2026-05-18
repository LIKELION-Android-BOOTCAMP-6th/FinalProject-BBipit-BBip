package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.model.NotificationDto
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSource
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    private val firestore = FirebaseFirestore.getInstance()

    // 알림 목록 조회
    override suspend fun getNotificationList(userId: String): Result<List<Notification>> {
        return try {
            val response = dataSource.fetchNotification(userId)
            Result.Success(response.map { (id, dto) -> dto.toEntity(id) })
        } catch (e: Exception) {
            Log.e("NotificationRepository", "알림 목록 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알림 목록을 가져오지 못했습니다."))
        }
    }

    override suspend fun markNotificationsAsRead(
        type: String,
        id: String?
    ): Result<Boolean> {
        val data = hashMapOf(
            "type" to type,
            "notificationId" to id
        )
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("markNotificationsAsRead")
                .call(data)
                .await()
            val res = result.data as? Map<*, *>
            Result.Success(res?.get("success") as? Boolean ?: false)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "알림 읽음 처리 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알림 읽음 처리 중 오류 발생"))
        }
    }

    // 실시간 구독
    override fun observeNotification(userId: String, onNew: (Notification) -> Unit): ListenerRegistration {
        val query = firestore.collection("Notifications")
            .document(userId)
            .collection("Notification")
            .orderBy("created_at", Query.Direction.DESCENDING)

        return query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            snapshot.documentChanges.forEach { change ->
                if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                    val dto = change.document.toObject(NotificationDto::class.java)
                    dto?.toEntity(change.document.id)?.let { onNew(it) }
                }
            }
        }
    }

    //
    override fun observeNotificationList(userId: String): Flow<List<Notification>> {
        return callbackFlow {
            val query = firestore.collection("Notifications")
                .document(userId)
                .collection("Notification")
                .orderBy("created_at", Query.Direction.DESCENDING)

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        val dto = doc.toObject(NotificationDto::class.java)
                        dto?.toEntity(doc.id)
                    }
                    trySend(items)
                    Log.d("NotificationRepo", "실시간 알림 스트림 갱신: ${items.size}건")
                }
            }

            awaitClose { listener.remove() }
        }
    }
    // 알림 삭제
    override suspend fun deleteNotifications(userId: String, id: String?): Result<Unit> {
        val data = hashMapOf(
            "type" to if (id == null) "all" else "single",
            "notificationId" to id
        )
        return try {
            firebaseFunctions
                .getHttpsCallable("deleteNotifications")
                .call(data)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "알림 삭제 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알림 삭제 실패"))
        }
    }
}