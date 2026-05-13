package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.NotiDto
import com.bbip.bbipit.domain.entity.NotiItem

fun NotiDto.toEntity(id: String): NotiItem {
    val createdAtMillis = created_at?.toDate()?.time ?: 0L
    val isExpired = if (type == "WALKIE") {
        System.currentTimeMillis() - createdAtMillis > 3 * 60 * 60 * 1000L
    } else {
        false
    }

    return NotiItem(
        id = id,
        type = type,
        senderId = sender_id,
        senderName = sender_name,
        content = content,
        audioUrl = audio_url,
        isRead = is_read,
        createdAt = created_at
    )
}