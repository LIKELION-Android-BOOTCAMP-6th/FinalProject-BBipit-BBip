package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.Notification
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow

/**
 * 알림 관련 데이터 처리를 담당하는 리포지토리입니다.
 * 알림 목록 조회, 읽음 처리, 실시간 관찰 기능을 수행합니다.
 */
interface NotificationRepository {
    // 알림 목록 조회
    suspend fun getNotificationList(userId: String): Result<List<Notification>>

    // 알림 읽음 처리
    suspend fun markNotificationsAsRead(type: String, notificationId: String?): Result<Boolean>

    // 새 알림 실시간 구독
    fun observeNotification(userId: String, onNew: (Notification) -> Unit): ListenerRegistration

    fun observeNotificationList(userId: String): Flow<List<Notification>>

    // 알림 삭제
    suspend fun deleteNotifications(userId: String, notiId: String?): Result<Unit>
}