package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
data class MessageDto(
    @get:PropertyName("sender_id") @set:PropertyName("sender_id")
    var senderId: String = "",

    @get:PropertyName("content") @set:PropertyName("content")
    var content: String = "",

    @get:PropertyName("sent_at") @set:PropertyName("sent_at")
    var sentAt: Timestamp? = null,

    @get:PropertyName("is_read") @set:PropertyName("is_read")
    var isRead: Boolean = false
)
