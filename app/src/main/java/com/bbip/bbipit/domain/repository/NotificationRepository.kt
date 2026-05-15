package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.domain.entity.Notification

interface NotificationRepository {
    suspend fun getNotiList(userId: String): Result<List<Notification>>
}