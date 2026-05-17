package com.bbip.bbipit.domain.usecase

import com.bbip.bbipit.core.result.Result // 🚨 [추가] 프로젝트 커스텀 Result를 명시적으로 임포트합니다.
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationListUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    // repository.getNotificationList(userId)가 우리가 만든 Result를 반환하므로 리턴 타입이 일치하게 됩니다.
    suspend operator fun invoke(userId: String): Result<List<Notification>> {
        return repository.getNotificationList(userId)
    }
}