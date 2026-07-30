package com.bbip.bbipit.data

data class WatchHistory (
    val id: String, // 문서 ID
    val userNickname: String, // 작성자 닉네임
    val userProfileImage: String, // 작성자 프로필 사진
    val placeName: String, // 장소명
    val userId: String, // 작성자 ID
    val category: String, // 카테고리
    val latitude: Double, // 위도
    val longitude: Double, // 경도
    val createdAt: Long, // 생성 시간
) {
    val expiryTime: Long
        get() = createdAt + (24 * 60 * 60 * 1000)

    // 만료 여부 반환
    fun isExpired(now: Long): Boolean {
        return now >= expiryTime
    }
}