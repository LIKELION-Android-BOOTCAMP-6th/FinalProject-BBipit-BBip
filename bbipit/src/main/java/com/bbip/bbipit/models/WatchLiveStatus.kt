package com.bbip.bbipit.models

/**
 * 워치 실시간 유저 상태 데이터 클래스
 */
data class WatchLiveStatus(
    val uid: String,
    val nickname: String = "",
    val profileImageUrl: String = "",
    val status: String = "",
    val isOnline: Boolean = false,
    val currentRoomId: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // 상태 업데이트 시간
    val updatedAt: Long = 0L,
    // 캐시 데이터 사용 여부
    val isFromCache: Boolean = false,
    val isSharing: Boolean = true
)