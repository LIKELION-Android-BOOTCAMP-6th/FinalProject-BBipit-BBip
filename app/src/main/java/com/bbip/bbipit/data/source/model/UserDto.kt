package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class UserDto(
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",

    @get:PropertyName("profile_image_url") @set:PropertyName("profile_image_url")
    var profileImageUrl: String = "",

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "",

    @get:PropertyName("is_sharing") @set:PropertyName("is_sharing")
    var isSharing: Boolean = false,

    @get:PropertyName("is_online") @set:PropertyName("is_online")
    var isOnline: Boolean = false,

    @get:PropertyName("fcm_token") @set:PropertyName("fcm_token")
    var fcmToken: String = "",

    @get:PropertyName("last_active") @set:PropertyName("last_active")
    var lastActive: Timestamp? = null,

    @get:PropertyName("friendUids") @set:PropertyName("friendUids")
    var friendUids: List<String> = emptyList()
)
