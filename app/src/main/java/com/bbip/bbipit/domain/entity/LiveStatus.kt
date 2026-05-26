package com.bbip.bbipit.domain.entity

/**
 * 실시간 위치 및 상태 정보 엔티티
 */
data class LiveStatus(
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