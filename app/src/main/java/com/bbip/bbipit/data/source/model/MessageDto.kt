package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * 채팅 메시지 정보 데이터 전송 객체
 */
data class MessageDto(
    // 발신자 고유 식별자
    @get:PropertyName("sender_id") @set:PropertyName("sender_id")
    var senderId: String = "",

    // 메시지 본문 내용
    @get:PropertyName("content") @set:PropertyName("content")
    var content: String = "",

    // 메시지 전송 시간
    @get:PropertyName("sent_at") @set:PropertyName("sent_at")
    var sentAt: Timestamp? = null,

    // 메시지 읽음 여부
    @get:PropertyName("is_read") @set:PropertyName("is_read")
    var isRead: Boolean = false
)