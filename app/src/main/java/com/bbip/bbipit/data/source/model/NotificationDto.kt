package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp

/**
 * 시스템 및 서비스 알림 정보 데이터 전송 객체
 */
data class NotificationDto(
    val id: String = "",

    // 알림 카테고리 종류
    val type: String = "",

    // 발신자 고유 식별자
    val sender_id: String = "",

    // 발신자 표시 이름
    val sender_name: String = "",

    val sender_url: String = "",

    // 알림 본문 내용
    val content: String = "",

    val voice_id: String? = null,

    val voice_isPlayed: Boolean = false,

    val voice_duration: Int = 0,

    // 연동된 채팅방 식별자
    val room_id: String? = null,

    // 알림 읽음 여부
    val is_read: Boolean = false,

    // 알림 생성 시간
    val created_at: Timestamp? = null,

    // 알림 만료 시간
    val expires_at: Timestamp? = null
)