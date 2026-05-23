package com.bbip.bbipit.domain.entity

/**
 * 채팅방 정보 엔티티
 */
data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMsg: String = "",
    val updatedAt: Long = 0L,
    val unreadCounts: Map<String, Int> = emptyMap()
)