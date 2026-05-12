package com.bbip.bbipit.domain.entity

data class CurrentUser(
    val id: String = "",
    val nickname: String = "",
    val profileImageUrl: String = "",
    val status: String = "",
    val isSharing: Boolean = true, //위치 공유 여부
    val isOnline: Boolean = true, //온라인 여부
    val fcmToken: String = "",
    val lastActive: Long = 0L,
)
