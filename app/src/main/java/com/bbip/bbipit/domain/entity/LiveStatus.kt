package com.bbip.bbipit.domain.entity

/**
 * 사용자 실시간 위치 좌표, 접속 유무 및 활성화 채팅방 문맥 포함 순수 코틀린 기반 도메인 엔티티 클래스
 */
data class LiveStatus(
    val uid: String,
    val nickname: String = "",
    val profileImageUrl: String = "",
    val status: String = "",
    val isOnline: Boolean = false,
    val currentRoomId: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // 파이어베이스 타임스탬프 플랫폼 결합도 완화 목적의 순수 롱(Long) 타입 밀리초 단위 갱신 시점 데이터
    val updatedAt: Long = 0L,
    // 파이어스토어 로컬 오프라인 캐시 구조 파싱 인출 정보 여부 판별 상태 플래그
    val isFromCache: Boolean = false
)