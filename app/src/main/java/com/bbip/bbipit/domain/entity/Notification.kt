package com.bbip.bbipit.domain.entity

/**
 * 알림 정보 엔티티
 */
data class Notification(
    val id: String = "",
    val type: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val audioId: String = "",
    val audioUrl: String = "",
    val createdAt: Long = 0L,
    val roomId: String = "",
    val isRead: Boolean = false,
    val expiresAt: Long?
) {
    /**
     * 알림 만료 여부 반환 프로퍼티
     */
    val isExpired: Boolean
        get() {
            if (type != "WALKIE") return false

            val now = System.currentTimeMillis()
            // 만료 시간을 3시간으로 계산
            val expireMillis = createdAt + (3 * 60 * 60 * 1000L)

            return expireMillis != 0L && now > expireMillis
        }
}