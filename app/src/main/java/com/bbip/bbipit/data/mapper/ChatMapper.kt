package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.ChatRoomDto
import com.bbip.bbipit.data.source.model.MessageDto
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.entity.ChatMessage

fun ChatRoomDto.toEntity(id: String): ChatRoom = ChatRoom(
    roomId = id,
    participants = participants,
    lastMsg = lastMsg,
    updatedAt = updatedAt?.toDate()?.time ?: 0L,
    unreadCounts = unreadCounts
)

fun MessageDto.toEntity(id: String): ChatMessage = ChatMessage(
    msgId = id,
    senderId = senderId,
    content = content,
    sentAt = sentAt?.toDate()?.time ?: 0L,
    isRead = isRead
)
