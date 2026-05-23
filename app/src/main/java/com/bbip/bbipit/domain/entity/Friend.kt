package com.bbip.bbipit.domain.entity

/**
 * 친구 프로필 정보 엔티티
 */
data class Friend (
    val uid: String = "",
    val nickname: String = "",
    val profile_image_url: String = "",
    val status: String = "",
    val isOnline: Boolean = false,
    val friendshipStatus: String = ""
)