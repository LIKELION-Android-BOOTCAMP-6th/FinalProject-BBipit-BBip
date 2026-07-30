package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * 사용자 계정 정보 데이터 전송 객체
 */
data class UserDto(
    // 사용자 닉네임
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",

    // 프로필 이미지 주소
    @get:PropertyName("profile_image_url") @set:PropertyName("profile_image_url")
    var profileImageUrl: String = "",

    // 상태 메시지
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "",

    // 위치 공유 활성화 여부
    @get:PropertyName("is_sharing") @set:PropertyName("is_sharing")
    var isSharing: Boolean = false,

    // 온라인 접속 여부
    @get:PropertyName("is_online") @set:PropertyName("is_online")
    var isOnline: Boolean = false,

    // 알림 푸시 토큰
    @get:PropertyName("fcm_token") @set:PropertyName("fcm_token")
    var fcmToken: String? = null,

    // 최종 활성화 시간
    @get:PropertyName("last_active") @set:PropertyName("last_active")
    var lastActive: Timestamp? = null,

    // 친구 고유 식별자 목록
    @get:PropertyName("friendUids") @set:PropertyName("friendUids")
    var friendUids: List<String> = emptyList(),

    // 로그인 플랫폼 종류
    @get:PropertyName("login_type") @set:PropertyName("login_type")
    var loginType: String = "",

    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("user_code") @set:PropertyName("user_code")
    var userCode: String = "",

    @get:PropertyName("current_session_id") @set:PropertyName("current_session_id")
    var sessionId: String? = null,
)