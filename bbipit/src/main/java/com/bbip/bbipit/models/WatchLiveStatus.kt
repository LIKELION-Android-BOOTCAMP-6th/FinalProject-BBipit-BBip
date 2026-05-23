package com.bbip.bbipit.models

/**
 * 워치 실시간 유저 상태 데이터 클래스
 */
data class WatchLiveStatus(
    val uid: String,                  // 유저 고유 식별자
    val nickname: String,             // 닉네임
    val profileImageUrl: String = "", // 프로필 이미지 URL
    val status: String = "",          // 상태 메시지
    val latitude: Double,             // 위도
    val longitude: Double,            // 경도
    val isOnline: Boolean             // 온라인 여부
)