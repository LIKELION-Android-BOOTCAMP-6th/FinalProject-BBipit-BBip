package com.bbip.bbipit.data.model
import com.google.firebase.firestore.PropertyName

/**
 DB와 통신 시 사용 (예시용 확정 X)
 */
data class UserDto(
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",

    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",

    @get:PropertyName("profile_image_url") @set:PropertyName("profile_image_url")
    var profileImageUrl: String = "",

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "",

    @get:PropertyName("is_sharing") @set:PropertyName("is_sharing")
    var isSharing: Boolean = true,

    @get:PropertyName("is_online") @set:PropertyName("is_online")
    var isOnline: Boolean = true,

    @get:PropertyName("fcm_token") @set:PropertyName("fcm_token")
    var fcmToken: String = "",

    @get:PropertyName("last_active") @set:PropertyName("last_active")
    var lastActive: Long = 0L,



)