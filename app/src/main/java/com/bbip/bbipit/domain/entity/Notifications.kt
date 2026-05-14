package com.bbip.bbipit.domain.entity

import com.google.firebase.Timestamp

data class Notifications(
    val notiId: String = "",
    val type: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val audioUrl: String = "",
    val createdAt: Timestamp? = null,
    val roomId: String = "",
    val isRead: Boolean = false,
    val expiresAt: Timestamp? = null
) {
    val isExpired: Boolean
        get() {
            if (type != "WALKIE") return false

            val now = System.currentTimeMillis()
            val expireMillis = expiresAt?.toDate()?.time
                ?: ((createdAt?.toDate()?.time ?: 0L) + (3 * 60 * 60 * 1000L))

            return expireMillis != 0L && now > expireMillis
        }
}