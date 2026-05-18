package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class ChatRoomDto(
    @get:PropertyName("participants") @set:PropertyName("participants")
    var participants: List<String> = emptyList(),

    // 파이어베이스 필드명과 일치시키기 위해
    @get:PropertyName("last_message") @set:PropertyName("last_message")
    var lastMsg: String = "",

    @get:PropertyName("last_message_at") @set:PropertyName("last_message_at")
    var updatedAt: Timestamp? = null,

    @get:PropertyName("unread_counts") @set:PropertyName("unread_counts")
    var unreadCounts: Map<String, Int> = emptyMap()
)
