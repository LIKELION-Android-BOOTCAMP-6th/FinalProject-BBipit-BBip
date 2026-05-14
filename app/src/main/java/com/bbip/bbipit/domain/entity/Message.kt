package com.bbip.bbipit.domain.entity

// 개별 메시지 정보
data class Message(
    val msgId: String = "",
    val senderId: String = "",
    val content: String = "",
    val sentAt: Long = 0L,
    val isRead: Boolean = false
)