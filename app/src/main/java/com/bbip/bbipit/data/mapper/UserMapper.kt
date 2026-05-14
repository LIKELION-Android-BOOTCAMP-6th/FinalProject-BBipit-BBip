package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.UserDto
import com.bbip.bbipit.domain.entity.User
import com.google.firebase.Timestamp

/**
 UserDto와 User 간의 변환 담당
 */

fun UserDto.toDomain(id: String): User = User(
    id = id,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    status = status,
    isSharing = isSharing,
    isOnline = isOnline,
    fcmToken = fcmToken,
    lastActive = lastActive?.toDate()?.time ?: 0L,
    friendUids = friendUids
)

fun User.toDto(): UserDto = UserDto(
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    status = status,
    isSharing = isSharing,
    isOnline = isOnline,
    fcmToken = fcmToken,
    lastActive = if (lastActive != 0L) Timestamp(java.util.Date(lastActive)) else null,
    friendUids = friendUids
)
