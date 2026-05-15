// data/mapper/NotificationMapper.kt
package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.NotificationDto
import com.bbip.bbipit.domain.entity.Notification

fun NotificationDto.toEntity(id: String): Notification {
        return Notification(
        notificationId = id,
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