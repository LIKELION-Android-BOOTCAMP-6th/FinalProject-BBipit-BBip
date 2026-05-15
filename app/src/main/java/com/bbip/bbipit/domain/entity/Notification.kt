package com.bbip.bbipit.domain.entity

// ~~
data class Notification(
    val notificationId: String = "",
    val type: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val audioUrl: String = "",
    val createdAt: Long = 0L,
    val roomId: String = "",
    val isRead: Boolean = false,
    val expiresAt: Long?
) {
    val isExpired: Boolean
        get() {
            if (type != "WALKIE") return false

            val now = System.currentTimeMillis()
            // 만료 시간을 3시간으로 가정
            val expireMillis = createdAt + (3 * 60 * 60 * 1000L)

            return expireMillis != 0L && now > expireMillis
        }
}
