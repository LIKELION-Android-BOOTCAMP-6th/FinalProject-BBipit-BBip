package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.FriendshipDto
import com.bbip.bbipit.domain.entity.Friend

/**
 * 데이터 레이어 DTO 구조의 도메인 레이어 Friend 엔티티 규격 매핑 확장 함수
 */
fun FriendshipDto.toDomain(): Friend {
    return Friend(
        uid = this.uid,
        nickname = this.nickname,
        profile_image_url = this.profile_image_url,
        status = this.status,
        friendshipStatus = this.friendship_status
    )
}

/**
 * 파이어베이스 원격 데이터베이스 맵 객체의 FriendshipDto 규격 가공 확장 함수
 * 서버 응답 데이터 파싱 및 안전한 타입 캐스팅 전담 목적
 */
fun Map<String, Any>?.toFriendshipDto(): FriendshipDto {
    if (this == null) return FriendshipDto()
    return FriendshipDto(
        uid = this["friend_uid"] as? String ?: "",
        nickname = this["nickname"] as? String ?: "",
        profile_image_url = this["profile_image_url"] as? String ?: "",
        status = this["friendship_status"] as? String ?: "",
        friendship_status = this["friendship_status"] as? String ?: ""
    )
}