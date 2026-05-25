package com.bbip.bbipit.domain.entity

data class History (
    val id: String, // 문서 ID
    val userId: String, // 작성자 ID
    val userNickname: String, // 작성자 닉네임
    val userProfileImage: String, // 작성자 프로필 사진
    val category: String, // 카테고리
    val placeName: String, // 장소명
    val content: String, // 본문 내용
    val geohash: String, // 지오해시
    val imageUrl: String, // 이미지 URL
    val latitude: Double, // 위도
    val longitude: Double, // 경도
    val createdAt: Long, // 생성 시간
)