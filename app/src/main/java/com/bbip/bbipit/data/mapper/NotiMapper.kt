// data/mapper/NotiMapper.kt
package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.NotiDto
import com.bbip.bbipit.domain.entity.Notifications

fun NotiDto.toEntity(id: String): Notifications {
        return Notifications(
        notiId = id,
        type = type,
        senderId = sender_id,
        senderName = sender_name,
        content = content,
        audioUrl = voice_url,
        roomId = room_id,
        isRead = is_read,
        createdAt = created_at,
        expiresAt = expires_at,
    )
}