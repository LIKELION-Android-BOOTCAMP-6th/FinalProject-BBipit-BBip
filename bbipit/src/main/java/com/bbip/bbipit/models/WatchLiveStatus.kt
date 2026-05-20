package com.bbip.bbipit.models

/**
 * 워치 디스플레이 해상도 및 데이터 트래픽 최적화를 위한 실시간 유저 상태 정보 경량화 모델
 */
data class WatchLiveStatus(
    val uid: String,                  // 유저 고유 식별자
    val nickname: String,             // 지도 및 프로필 표시용 닉네임
    val profileImageUrl: String = "", // 원격 저장소 프로필 이미지 경로
    val status: String = "",          // 유저가 설정한 한 줄 상태 메시지
    val latitude: Double,             // 지도 매핑용 실시간 위도 좌표
    val longitude: Double,            // 지도 매핑용 실시간 경도 좌표
    val isOnline: Boolean             // 현재 앱 활성화 및 네트워크 접속 여부
)