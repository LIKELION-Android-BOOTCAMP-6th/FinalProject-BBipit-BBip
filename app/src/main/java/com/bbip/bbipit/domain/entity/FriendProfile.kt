package com.bbip.bbipit.domain.entity

// 친구 정보
data class FriendProfile(
    val uid: String,
    val nickname: String,
    val profileImageUrl: String,
    val status: String,
    val isOnline: Boolean,
    val lastActive: Long,
    val friendshipStatus: String,
    val updatedAt: Long
)