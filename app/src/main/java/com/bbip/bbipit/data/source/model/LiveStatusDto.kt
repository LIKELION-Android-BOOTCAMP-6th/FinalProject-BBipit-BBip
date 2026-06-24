package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp

/**
 * 실시간 위치 및 접속 상태 데이터 전송 객체
 */
data class LiveStatusDto(
    // 사용자 닉네임
    val nickname: String = "",

    // 프로필 이미지 주소
    val profileImageUrl: String = "",

    // 상태 메시지
    val status: String = "",

    // 온라인 접속 여부
    val isOnline: Boolean = false,

    // 현재 체류 중인 채팅방 식별자
    val currentRoomId: String? = null,

    // 위도 좌표
    val latitude: Double = 0.0,

    // 경도 좌표
    val longitude: Double = 0.0,

    // 상태 업데이트 시간
    val updatedAt: Timestamp? = null,
    val isSharing: Boolean = true,
    val sessionId: String? = null,
)