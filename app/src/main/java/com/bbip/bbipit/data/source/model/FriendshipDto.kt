package com.bbip.bbipit.data.source.model

/**
 * 사용자 친구 관계 및 프로필 정보 데이터 전송 객체
 */
data class FriendshipDto(
    // 친구 고유 식별자
    val uid: String = "",

    // 친구 닉네임
    val nickname: String = "",

    // 프로필 이미지 주소
    val profile_image_url: String = "",

    // 상태 메시지
    val status: String = "",

    // 온라인 접속 여부
    val is_online: Boolean = false,

    // 친구 관계 상태
    val friendship_status: String = ""
)