package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class VoiceMessageDto(
    @get:PropertyName("sender_id") @set:PropertyName("sender_id")
    var senderId: String = "",

    @get:PropertyName("voice_url") @set:PropertyName("voice_url")
    var voiceUrl: String = "",

    @get:PropertyName("duration") @set:PropertyName("duration")
    var duration: Int = 0,

    // 클라우드 함수 필드명(sent_at)에 맞게 수정
    @get:PropertyName("sent_at") @set:PropertyName("sent_at")
    var createdAt: Timestamp? = null,

    // 함수에서 추가할 경우 대비
    @get:PropertyName("receiver_id") @set:PropertyName("receiver_id")
    var receiverId: String = "",

    @get:PropertyName("is_read") @set:PropertyName("is_read")
    var isRead: Boolean = false
)