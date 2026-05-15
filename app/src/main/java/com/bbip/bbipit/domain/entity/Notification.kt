package com.bbip.bbipit.domain.entity

data class Notification(
    val notification: String = "",
    val type: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val audioUrl: String = "",
    val createdAt: Long = 0L,
    val roomId: String = "",
    val isRead: Boolean = false,
    val expiresAt: Long? = null
) {
    val isExpired: Boolean
        get() {
            if (type != "WALKIE") return false

            val now = System.currentTimeMillis()
            val expireMillis = expiresAt
                ?: if (createdAt != 0L) (createdAt + (3 * 60 * 60 * 1000L)) else 0L

            return expireMillis != 0L && now > expireMillis
        }
}
