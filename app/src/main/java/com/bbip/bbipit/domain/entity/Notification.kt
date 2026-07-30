package com.bbip.bbipit.domain.entity

/**
 * 알림 정보 엔티티
 */
data class Notification(
    val id: String = "",
    val type: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val profileImage: String = "",
    val content: String = "",
    val audioId: String = "",
    val isPlayed: Boolean = false,
    val createdAt: Long = 0L,
    val roomId: String = "",
    val isRead: Boolean = false,
    val expiresAt: Long = 0L,
    // 최초 구독 상태
    val isInitial: Boolean = false
) {
    /**
     * 알림 만료 여부 반환 프로퍼티
     */
    val isExpired: Boolean
        get() {
            if (type != "WALKIE") return false

            val now = System.currentTimeMillis()
            return now > expiresAt
        }
}