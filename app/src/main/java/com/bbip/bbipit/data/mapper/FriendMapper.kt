package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.FriendshipDto
import com.bbip.bbipit.domain.entity.Friend

/**
 * DTO 객체의 도메인 엔티티 변환 확장 함수
 */
fun FriendshipDto.toDomain(): Friend {
    return Friend(
        uid = this.uid,
        nickname = this.nickname,
        profile_image_url = this.profile_image_url,
        status = this.status,
        isOnline = this.is_online,
        friendshipStatus = this.friendship_status
    )
}

/**
 * 맵 객체의 DTO 변환 확장 함수
 */
fun Map<String, Any>?.toFriendshipDto(): FriendshipDto {
    // 데이터가 없으면 빈 객체 반환
    if (this == null) return FriendshipDto()

    // 맵 데이터를 DTO 구조로 변환
    return FriendshipDto(
        uid = this["friend_uid"] as? String ?: "",
        nickname = this["nickname"] as? String ?: "",
        profile_image_url = this["profile_image_url"] as? String ?: "",
        status = this["status"] as? String ?: "",
        is_online = this["is_online"] as? Boolean ?: false,
        friendship_status = this["friendship_status"] as? String ?: ""
    )
}