package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.core.extension.urlMapper
import com.bbip.bbipit.data.source.model.UserDto
import com.bbip.bbipit.domain.entity.User
import com.google.firebase.Timestamp

/**
 * UserDto를 User 엔티티로 변환하는 함수
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
    friendUids = friendUids,
    loginType = loginType,
    email = email,
    userCode = userCode,
)

/**
 * User 엔티티를 UserDto로 변환하는 함수
 */
fun User.toDto(): UserDto = UserDto(
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    status = status,
    isSharing = isSharing,
    isOnline = isOnline,
    fcmToken = fcmToken,
    lastActive = if (lastActive != 0L) Timestamp(java.util.Date(lastActive)) else null,
    friendUids = friendUids,
    loginType = loginType,
    email = email,
    userCode = userCode,
    sessionId = sessionId
)

/**
 * Map 데이터를 User 엔티티로 변환하는 함수
 */
fun Map<String, Any>.toDomain(): User {
    // 수신된 맵 데이터를 엔티티 구조로 변환
    return User(
        id = this["uid"] as? String ?: "",
        nickname = this["nickname"] as? String ?: "익명",
        profileImageUrl = (this["profile_image_url"] as? String ?: "").urlMapper(),
        status = this["status"] as? String ?: "",
        isSharing = this["is_sharing"] as? Boolean ?: false,
        isOnline = this["is_online"] as? Boolean ?: false,
        fcmToken = this["fcm_token"] as? String,
        lastActive = (this["last_active"] as? Number)?.toLong() ?: 0L,
        friendUids = emptyList(),
        loginType = this["login_type"] as? String ?: "",
        email = this["email"] as? String ?: "",
        userCode = this["user_code"] as? String ?: "",
        sessionId = this["current_session_id"] as String
    )
}