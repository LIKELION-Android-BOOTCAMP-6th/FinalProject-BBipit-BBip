package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * 음성 무전 메시지 정보 데이터 전송 객체
 */
data class VoiceMessageDto(
    // 발신자 고유 식별자
    @get:PropertyName("sender_id") @set:PropertyName("sender_id")
    var senderId: String = "",

    // 음성 파일 주소
    @get:PropertyName("voice_url") @set:PropertyName("voice_url")
    var voiceUrl: String = "",

    // 음성 재생 시간
    @get:PropertyName("duration") @set:PropertyName("duration")
    var duration: Int = 0,

    // 메시지 생성 시간
    @get:PropertyName("sent_at") @set:PropertyName("sent_at")
    var createdAt: Timestamp? = null,

    // 수신자 고유 식별자
    @get:PropertyName("receiver_id") @set:PropertyName("receiver_id")
    var receiverId: String = "",

    // 메시지 읽음 여부
    @get:PropertyName("is_read") @set:PropertyName("is_read")
    var isRead: Boolean = false
)