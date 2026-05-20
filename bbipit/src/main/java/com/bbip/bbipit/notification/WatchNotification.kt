package com.bbip.bbipit.notification

data class Notification(
    val id: String,
    val senderName: String,
    val type: String,
    val content: String,
    val createdAt: Long,
    val roomId: String? = null,
    val isRead: Boolean = false,
    val isExpired: Boolean = false,
    val expiresAt: Long? = null
)