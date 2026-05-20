package com.bbip.bbipit.models

/**
 * Wearable 데이터 레이어 통신 및 수신 팝업 출력을 위한 음성 메시지 데이터 모델
 */
data class WatchVoiceData(
    val messageId: String,        // 읽음 처리 동기화를 위한 메시지 고유 식별자
    val voiceUrl: String,         // 오디오 스트리밍 재생을 위한 원격 파일 주소
    val senderProfileUrl: String, // 수신 다이얼로그 표시용 송신자 프로필 이미지 경로
    val senderName: String        // 수신 다이얼로그 표시용 송신자 닉네임
)