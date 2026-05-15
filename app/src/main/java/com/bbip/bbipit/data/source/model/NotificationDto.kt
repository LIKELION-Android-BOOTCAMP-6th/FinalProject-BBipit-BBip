package com.bbip.bbipit.data.source.model

data class NotificationDto(
    val type: String = "",
    val sender_id: String = "",
    val sender_name: String = "",
    val content: String = "",
    val voice_url: String = "",
    val room_id: String = "",
    val is_read: Boolean = false,
    val created_at: Long,
    val expires_at: Long? = null
)