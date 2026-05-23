package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.LiveStatusDto
import com.bbip.bbipit.domain.entity.LiveStatus
import com.google.firebase.Timestamp

/**
 * 도메인 엔티티의 DTO 변환 확장 함수
 */
fun LiveStatus.toDto(): LiveStatusDto {
    // 엔티티 데이터를 DTO 구조로 변환
    return LiveStatusDto(
        nickname = this.nickname,
        profileImageUrl = this.profileImageUrl,
        status = this.status,
        isOnline = this.isOnline,
        currentRoomId = this.currentRoomId,
        latitude = this.latitude,
        longitude = this.longitude,
        updatedAt = Timestamp.now()
    )
}

/**
 * 맵 객체의 DTO 변환 확장 함수
 */
fun Map<String, Any>?.toDto(): LiveStatusDto {
    // 데이터가 없으면 빈 객체 반환
    if (this == null) return LiveStatusDto()

    // 맵 데이터를 DTO 구조로 변환
    return LiveStatusDto(
        nickname = this["nickname"] as? String ?: "",
        profileImageUrl = this["profile_image_url"] as? String ?: "",
        status = this["status"] as? String ?: "",
        isOnline = this["is_online"] as? Boolean ?: false,
        currentRoomId = this["current_room_id"] as? String,
        latitude = (this["latitude"] as? Number)?.toDouble() ?: 0.0,
        longitude = (this["longitude"] as? Number)?.toDouble() ?: 0.0,
        updatedAt = this["updated_at"] as? Timestamp
    )
}

/**
 * DTO 객체의 도메인 엔티티 변환 확장 함수
 */
fun LiveStatusDto.toDomain(uid: String, isFromCache: Boolean): LiveStatus {
    // DTO 데이터를 엔티티 구조로 변환
    return LiveStatus(
        uid = uid,
        nickname = this.nickname,
        profileImageUrl = this.profileImageUrl,
        status = this.status,
        isOnline = this.isOnline,
        currentRoomId = this.currentRoomId,
        latitude = this.latitude,
        longitude = this.longitude,
        updatedAt = this.updatedAt?.toDate()?.time ?: 0L,
        isFromCache = isFromCache
    )
}