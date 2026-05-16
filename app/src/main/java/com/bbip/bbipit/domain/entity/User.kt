package com.bbip.bbipit.domain.entity

/**
 * 사용자 정보 도메인 엔티티
 * 사용자 식별자, 닉네임, 프로필 이미지, 상태 메시지, 공유 상태, 온라인 여부 등 계정 정보 포함
 */
data class User(
    // 사용자 고유 식별자
    val id: String = "",
    // 사용자 표시 닉네임
    val nickname: String = "",
    // 프로필 이미지 웹 주소
    val profileImageUrl: String = "",
    // 사용자 상태 메시지
    val status: String = "",
    // 위치 공유 활성화 여부
    val isSharing: Boolean = false,
    // 현재 접속 온라인 상태
    val isOnline: Boolean = false,
    // 푸시 알림용 FCM 토큰
    val fcmToken: String = "",
    // 마지막 활동 시간(타임스탬프)
    val lastActive: Long = 0L,
    // 등록된 친구 목록(사용자 식별자 리스트)
    val friendUids: List<String> = emptyList()
)