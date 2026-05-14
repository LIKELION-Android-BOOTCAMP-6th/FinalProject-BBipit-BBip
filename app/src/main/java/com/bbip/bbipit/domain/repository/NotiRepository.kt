package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.domain.entity.Notifications

interface NotiRepository {
    suspend fun getNotiList(userId: String): Result<List<Notifications>>
}