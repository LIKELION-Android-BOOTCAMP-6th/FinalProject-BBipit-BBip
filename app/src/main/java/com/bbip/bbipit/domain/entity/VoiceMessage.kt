package com.bbip.bbipit.domain.entity

data class VoiceMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val voiceUrl: String = "",
    val duration: Int = 0,
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)
