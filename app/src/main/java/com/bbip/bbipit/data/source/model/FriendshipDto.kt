package com.bbip.bbipit.data.source.model

/**
 * 서버 통신 및 데이터 적재 규격 맞춤 사용자 간 친구 관계 및 기초 프로필 정보 저장 데이터 전송 객체(DTO)
 */
data class FriendshipDto(
    val uid: String = "",
    val nickname: String = "",
    val profile_image_url: String = "",
    val status: String = ""
)