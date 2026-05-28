package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.Notification
import kotlinx.coroutines.flow.StateFlow

/**
 * 알림 관련 데이터 처리를 담당하는 리포지토리입니다.
 * 알림 목록 조회, 읽음 처리, 실시간 관찰 기능을 수행합니다.
 */
interface NotificationRepository {

    val notifications: StateFlow<List<Notification>>
    val uiReadIds: StateFlow<Set<String>>


    // 구독 즉시 전체 문서를 수신하여 캐시에 보관
    fun startObserving(userId: String)

    // 구독 중단 (로그아웃 시 호출)
    fun stopObserving()

    // 로그아웃 시 캐시 완전 초기화
    fun clearCache()

    // 알림 목록 조회
    suspend fun getNotificationList(userId: String): Result<List<Notification>>

    // 알림 읽음 처리
    suspend fun markNotificationsAsRead(type: String, notificationId: String?): Result<Boolean>

    // 단건 알림 읽음 처리
    suspend fun markAsRead(notificationId: String): Result<Unit>

    // 알림 삭제
    suspend fun deleteNotifications(userId: String, notificationId: String?): Result<Unit>

    // 무전 알림 → VoiceMessage 재생 처리
    suspend fun playWalkieNotification(notification: Notification, receiverId: String)

    // Intent에서 추출한 데이터로 무전 즉시 재생
    fun playWalkie(intent: android.content.Intent, receiverId: String)
    suspend fun markVoiceNotiAsPlayed(notificationId: String): Boolean
}