package com.bbip.bbipit.domain.entity

/**
 * 서비스 내 상호 관계 수립 주변 사용자 기초 프로필 정보 표현 도메인 엔티티 클래스
 */
data class Friend (
    val uid: String = "",
    val nickname: String = "",
    val profile_image_url: String = "",
    val status: String = "",
    val isOnline: Boolean = false
)