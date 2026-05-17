package com.bbip.bbipit.data.source.model
import com.google.firebase.Timestamp
data class NotificationDto(
    val type: String = "",
    val sender_id: String = "",
    val sender_name: String = "",
    val content: String = "",
    val voice_url: String? = null,
    val room_id: String? = null,
    val is_read: Boolean = false,
    val created_at: Timestamp? = null,
    val expires_at: Timestamp? = null
)