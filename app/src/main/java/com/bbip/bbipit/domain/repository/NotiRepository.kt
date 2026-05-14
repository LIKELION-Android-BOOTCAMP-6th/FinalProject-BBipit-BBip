package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.domain.entity.NotiItem

interface NotiRepository {
    suspend fun getNotiList(userId: String): Result<List<NotiItem>>

    /**
     * 알림 읽음 처리
     * @param type 'all' 또는 'single'
     * @param notificationId 'single'일 경우 필수
     */
    suspend fun markNotificationsAsRead(type: String, notificationId: String? = null): Result<Boolean>

    /**
     * 알림 삭제
     * @param type 'all' 또는 'single'
     * @param notificationId 'single'일 경우 필수
     */
    suspend fun deleteNotifications(type: String, notificationId: String? = null): Result<Boolean>
}