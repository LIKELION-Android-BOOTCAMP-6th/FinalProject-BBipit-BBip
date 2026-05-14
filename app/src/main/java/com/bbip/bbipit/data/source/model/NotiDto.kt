package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp

data class NotiDto(
    val type: String = "",
    val sender_id: String = "",
    val sender_name: String = "",
    val content: String = "",
    val voice_url: String = "",
    val room_id: String = "",
    val is_read: Boolean = false,
    val created_at: Timestamp? = null,
    val expires_at: Timestamp? = null
)