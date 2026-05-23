package com.bbip.bbipit.models

/**
 * 워치 음성 데이터 클래스
 */
data class WatchVoiceData(
    val messageId: String,        // 메시지 고유 식별자
    val voiceUrl: String,         // 음성 파일 URL
    val senderProfileUrl: String, // 송신자 프로필 이미지 URL
    val senderName: String        // 송신자 닉네임
)