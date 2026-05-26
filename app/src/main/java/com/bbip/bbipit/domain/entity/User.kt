package com.bbip.bbipit.domain.entity

/**
 * 유저 계정 정보 엔티티
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
    val friendUids: List<String> = emptyList(),
    // 로그인 타입(EMAIL, GOOGLE, KAKAO)
    val loginType: String = "",
    val email: String= "",
    //현재 세션 정보(중복 로그인용)
    val sessionId: String = "",
    //친구요청용 코드
    val userCode: String = ""
)