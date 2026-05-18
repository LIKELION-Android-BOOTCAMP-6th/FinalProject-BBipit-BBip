package com.bbip.bbipit.domain.entity

// 채팅방 목록 정보
data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMsg: String = "",
    val updatedAt: Long = 0L, // Timestamp를 Long(ms)으로 변환하여 관리 권장
    val unreadCounts: Map<String, Int> = emptyMap()
)