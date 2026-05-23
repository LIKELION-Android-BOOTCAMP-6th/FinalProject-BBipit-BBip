package com.bbip.bbipit.domain.entity

/**
 * 채팅방 생성 결과 정보 엔티티
 */
data class ChatRoomResult(
    val success: Boolean,
    val roomId: String?,
    val message: String
)