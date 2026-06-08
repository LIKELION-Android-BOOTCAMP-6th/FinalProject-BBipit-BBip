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
    // 특정 사용자가 이 히스토리에 좋아요를 눌렀는지 여부 반환
    fun isLikedByUser(myUid: String): Boolean = likedUserIds.contains(myUid)
}