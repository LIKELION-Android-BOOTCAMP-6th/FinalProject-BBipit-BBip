package com.bbip.bbipit.domain.entity

data class History (
    val id: String, // 문서 ID
    val userId: String, // 작성자 ID
    val userNickname: String, // 작성자 닉네임
    val userProfileImage: String, // 작성자 프로필 사진
    val category: String, // 카테고리
    val placeName: String, // 장소명
    val content: String, // 본문 내용
    val latitude: Double, // 위도
    val longitude: Double, // 경도
    val createdAt: Long, // 생성 시간
    val imageUrls: List<String>, // 사진들
    val likedUserIds: List<String>, // 좋아요 누른 유저 ID 리스트
) {
    // 만료 시간
    val expiryTime: Long
        get() = createdAt + (24 * 60 * 60 * 1000)

    // 만료까지 남은 시간 (밀리초 단위 반환)
    fun getRemainingTimeMillis(now: Long): Long {
        return (expiryTime - now).coerceAtLeast(0L)
    }
    // 만료까지 남은  시간에 표시될 텍스트 (유동적인 단위 반환)
    fun getRemainingHoursText(now: Long): String {
        val remainingMillis = getRemainingTimeMillis(now)
        val remainingHours = remainingMillis / (1000 * 60 * 60)
        val remainingMinutes = (remainingMillis / (1000 * 60)) % 60

        return when {
            remainingMillis <= 0 -> "만료됨"
            remainingHours >= 1 -> "${remainingHours}시간 남음"
            else -> "${remainingMinutes}분 남음" // 1시간 미만 시 분 단위 표시
        }
    }
    // 만료 여부 반환
    fun isExpired(now: Long): Boolean {
        return now >= expiryTime
    }

    // 특정 사용자가 이 히스토리에 좋아요를 눌렀는지 여부 반환
    fun isLikedByUser(myUid: String): Boolean = likedUserIds.contains(myUid)
}