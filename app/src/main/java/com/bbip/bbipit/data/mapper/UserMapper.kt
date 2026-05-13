package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.UserDto
import com.bbip.bbipit.domain.entity.User

/**
 UserDto와 CurrentUser 간의 변환 담당
 */

fun UserDto.toDomain(): User = User(
    id = uid,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    status = status,
    isSharing = isSharing,
    isOnline = isOnline,
    fcmToken = fcmToken,
    lastActive = lastActive,
)

fun User.toDto(): UserDto = UserDto(
    uid = id,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    status = status,
    isSharing = isSharing,
    isOnline = isOnline,
    fcmToken = fcmToken,
    lastActive = lastActive,
)