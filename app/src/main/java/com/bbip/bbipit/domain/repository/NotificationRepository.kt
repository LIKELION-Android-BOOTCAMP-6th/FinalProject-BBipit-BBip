package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.domain.entity.Notification
import com.google.firebase.firestore.ListenerRegistration

interface NotificationRepository {
    suspend fun getNotificationList(userId: String): Result<List<Notification>>

    /**
     * 알림 읽음 처리
     * @param type 'all' 또는 'single'
     * @param notificationId 'single'일 경우 필수
     */
    suspend fun markNotificationsAsRead(type: String, notificationId: String? = null): Result<Boolean>

    fun observeNewNotification(userId: String, onNew: (Notification) -> Unit): ListenerRegistration
    /**
     * 알림 삭제
     * @param type 'all' 또는 'single'
     * @param notificationId 'single'일 경우 필수
     */

    suspend fun deleteNotification(userId: String, notiId: String): Result<Unit>
}
