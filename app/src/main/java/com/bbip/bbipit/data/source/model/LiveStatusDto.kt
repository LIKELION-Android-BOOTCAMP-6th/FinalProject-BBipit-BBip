package com.bbip.bbipit.data.source.model

import com.google.firebase.Timestamp

/**
 * 파이어베이스 원격 컬렉션 저장소 및 앱 물리 데이터 소스 계층 간 실시간 위경도, 접속 상태 정보 연동 담당 데이터 객체
 */
data class LiveStatusDto(
    val nickname: String = "",
    val profileImageUrl: String = "",
    val status: String = "",
    val isOnline: Boolean = false,
    val currentRoomId: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val updatedAt: Timestamp? = null
) {
    /**
     * 파이어베이스 서버 키-값 매핑 인터페이스 요구 규격 동기화 목적의 객체 내부 필드 맵(Map) 구조 직렬화 분해 변환 함수
     * 누락 방지 목적의 시간 값 널(Null) 상태 시 런타임 기준 실시간 서버 스탬프 코드 디폴트 대체 주입 처리 포함
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "nickname" to nickname,
            "profile_image_url" to profileImageUrl,
            "status" to status,
            "is_online" to isOnline,
            "current_room_id" to currentRoomId,
            "latitude" to latitude,
            "longitude" to longitude,
            "updated_at" to (updatedAt ?: Timestamp.now())
        )
    }
}