package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.UserDto
import com.bbip.bbipit.domain.entity.User
import com.google.firebase.Timestamp

/**
Dto -> Domain 변환
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

/**
Domain -> Dto 변환
 */
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

/**
Cloud Functions 응답(Map) -> Domain 변환
 */
fun Map<String, Any>.toDomain(): User {
    return User(
        id = this["uid"] as? String ?: "",
        nickname = this["nickname"] as? String ?: "익명",
        profileImageUrl = this["profile_image_url"] as? String ?: "",
        status = this["status"] as? String ?: "",
        isSharing = this["is_sharing"] as? Boolean ?: false, // DTO 필드명에 맞춰 추가
        isOnline = this["is_online"] as? Boolean ?: false,
        fcmToken = "", // 보안상 Functions에서 제외됨
        lastActive = (this["last_active"] as? Number)?.toLong() ?: 0L,
        friendUids = emptyList() // 보안상 Functions에서 제외됨
    )
}