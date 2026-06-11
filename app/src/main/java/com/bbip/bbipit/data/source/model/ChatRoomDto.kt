package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * 채팅방 정보 원격 데이터 전송 객체
 */
data class ChatRoomDto(
    // 채팅방 참여자 식별자 목록
    @get:PropertyName("participants") @set:PropertyName("participants")
    var participants: List<String> = emptyList(),

    // 마지막 수신 메시지 본문
    @get:PropertyName("last_message") @set:PropertyName("last_message")
    var lastMsg: String = "",

    // 마지막 메시지 전송 시간
    @get:PropertyName("last_message_at") @set:PropertyName("last_message_at")
    var updatedAt: Timestamp? = null,

    // 참여자별 미독 메시지 개수 맵
    @get:PropertyName("unread_counts") @set:PropertyName("unread_counts")
    var unreadCounts: Map<String, Int> = emptyMap(),

    // [추가] 상대방의 온라인 상태를 필드에서 직접 받아올 경우
    @get:PropertyName("is_online") @set:PropertyName("is_online")
    var isOnline: Boolean = false,

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Timestamp? = null
)