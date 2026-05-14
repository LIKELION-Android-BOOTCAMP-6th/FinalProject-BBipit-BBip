package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.NotificationDto
import com.bbip.bbipit.domain.entity.NotiItem

fun NotificationDto.toEntity(id: String): NotiItem {
    return NotiItem(
        id = id,
        type = type,
        senderId = senderId,
        senderName = senderName,
        content = content ?: "",
        audioUrl = audioUrl ?: "",
        isRead = isRead,
        createdAt = createdAt?.toDate()?.time ?: 0L,
        roomId = roomId ?: ""
    )
}
