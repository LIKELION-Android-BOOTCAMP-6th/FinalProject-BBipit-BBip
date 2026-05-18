package com.bbip.bbipit.domain.entity

// 개별 메시지 정보
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val content: String = "",
    val sentAt: Long = 0L,
    val isRead: Boolean = false
)