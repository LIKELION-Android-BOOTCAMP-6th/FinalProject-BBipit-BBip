package com.bbip.bbipit.domain.usecase

import com.bbip.bbipit.core.result.Result // 🚨 [추가] 프로젝트 커스텀 Result를 명시적으로 임포트합니다.
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationListUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
}