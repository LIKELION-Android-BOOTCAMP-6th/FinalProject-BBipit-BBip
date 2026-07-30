package com.bbip.bbipit.domain.entity

/**
 * 채팅방 정보 엔티티
 */
data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMsg: String = "",
    val updatedAt: Long = 0L,
    val unreadCounts: Map<String, Int> = emptyMap(),
    val lastSenderId: String = "", // 마지막 메시지 보낸 사람 ID (읽음 상태 판단용)
    val isOnline: Boolean = false // [추가]
)