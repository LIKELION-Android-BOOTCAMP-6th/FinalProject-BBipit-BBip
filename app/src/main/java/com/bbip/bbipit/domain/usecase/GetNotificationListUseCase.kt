package com.bbip.bbipit.domain.usecase

import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationListUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(userId: String): Result<List<Notification>> {
        return repository.getNotificationList(userId)
    }
}