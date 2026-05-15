package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class ChatRoomDto(
    @get:PropertyName("participants") @set:PropertyName("participants")
    var participants: List<String> = emptyList(),

    @get:PropertyName("last_msg") @set:PropertyName("last_msg")
    var lastMsg: String = "",

    @get:PropertyName("updated_at") @set:PropertyName("updated_at")
    var updatedAt: Timestamp? = null,

    @get:PropertyName("unread_counts") @set:PropertyName("unread_counts")
    var unreadCounts: Map<String, Int> = emptyMap()
)
