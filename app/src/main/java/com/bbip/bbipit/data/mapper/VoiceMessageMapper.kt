package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.VoiceMessageDto
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Map 데이터 -> VoiceMessageDto 변환 확장 함수
 */
fun Map<*, *>.toVoiceMessageDto(): VoiceMessageDto {
    val sentAtMillis = (this["sent_at"] as? Number)?.toLong() ?: 0L
    val timestamp = if (sentAtMillis > 0L) Timestamp(Date(sentAtMillis)) else null

    return VoiceMessageDto(
        senderId = this["sender_id"] as? String ?: "",
        senderName = this["sender_name"] as? String ?: "",
        senderProfileUrl = this["sender_profile_url"] as? String ?: "",
        voiceUrl = this["voice_url"] as? String ?: "",
        duration = (this["duration"] as? Number)?.toInt() ?: 0,
        receiverId = this["receiver_id"] as? String ?: "",
        createdAt = timestamp
    )
}

/**
 * VoiceMessageDto -> Domain Entity(VoiceMessage) 변환 확장 함수
 */
fun VoiceMessageDto.toDomainEntity(id: String, isInitial: Boolean = false): VoiceMessage {
    return VoiceMessage(
        id = id,
        senderId = this.senderId,
        senderName = this.senderName,
        senderProfileUrl = this.senderProfileUrl,
        receiverId = this.receiverId,
        voiceUrl = this.voiceUrl,
        duration = this.duration,
        createdAt = this.createdAt?.toDate()?.time ?: 0L,
        isInitial = isInitial
    )
}