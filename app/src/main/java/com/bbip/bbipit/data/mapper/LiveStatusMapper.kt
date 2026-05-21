package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.data.source.model.LiveStatusDto
import com.bbip.bbipit.domain.entity.LiveStatus
import com.google.firebase.Timestamp

/**
 * 도메인 레이어 실시간 상태 엔티티의 파이어베이스 통신용 DTO 객체 매핑 확장 함수 (쓰기용)
 * 데이터 적재 시점 기준 파이어베이스 현재 서버 타임스탬프 자동 주입 목적
 */
fun LiveStatus.toDto(): LiveStatusDto {
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
 * 파이어베이스 스냅샷 구조 Firestore Map 데이터의 DTO 전환 확장 함수 (읽기용)
 * 숫자형 좌표 데이터 유실 방지 목적의 넘버 형식 조회 및 더블 타입 변환 처리 적용
 */
fun Map<String, Any>?.toDto(): LiveStatusDto {
    if (this == null) return LiveStatusDto()
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
 * 가공 완료 DTO 인스턴스의 플랫폼 비종속적 순수 코틀린 도메인 엔티티 최종 변환 확장 함수 (읽기용)
 * 파이어베이스 전용 데이터 객체의 코틀린 범용 롱(Long) 타입 밀리초 단위 파싱 연산 및 저장 목적
 */
fun LiveStatusDto.toDomain(uid: String, isFromCache: Boolean): LiveStatus {
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