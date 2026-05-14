package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class NotificationDto(
    @get:PropertyName("type") @set:PropertyName("type")
    var type: String = "",

    @get:PropertyName("sender_id") @set:PropertyName("sender_id")
    var senderId: String = "",

    @get:PropertyName("sender_name") @set:PropertyName("sender_name")
    var senderName: String = "",

    @get:PropertyName("content") @set:PropertyName("content")
    var content: String? = null,

    @get:PropertyName("audio_url") @set:PropertyName("audio_url")
    var audioUrl: String? = null,

    @get:PropertyName("room_id") @set:PropertyName("room_id")
    var roomId: String? = null,

    @get:PropertyName("is_read") @set:PropertyName("is_read")
    var isRead: Boolean = false,

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Timestamp? = null
)
