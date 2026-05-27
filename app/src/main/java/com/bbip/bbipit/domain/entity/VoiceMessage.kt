package com.bbip.bbipit.domain.entity

/**
 * 음성 무전 메시지 정보 엔티티
 */
data class VoiceMessage(
    // 메시지 고유 식별자
    val id: String = "",
    // 발신자 고유 식별자
    val senderId: String = "",
    // 수신자 고유 식별자
    val receiverId: String = "",
    // 오디오 파일 웹 경로
    val voiceUrl: String = "",
    // 오디오 재생 시간(초)
    val duration: Int = 0,
    // 메시지 읽음 여부
    val isRead: Boolean = false,
    // 메시지 생성 시간(타임스탬프)
    val createdAt: Long = 0L,
    // 최초 구독 상태
    val isInitial: Boolean = false
)