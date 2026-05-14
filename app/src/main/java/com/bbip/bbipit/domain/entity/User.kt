package com.bbip.bbipit.domain.entity

data class User(
    val id: String = "",
    val nickname: String = "",
    val profileImageUrl: String = "",
    val status: String = "",
    val isSharing: Boolean = false,
    val isOnline: Boolean = false,
    val fcmToken: String = "",
    val lastActive: Long = 0L,
    val friendUids: List<String> = emptyList()
)
