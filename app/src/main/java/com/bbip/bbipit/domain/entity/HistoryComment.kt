package com.bbip.bbipit.domain.entity

data class HistoryComment (
    val id: String,              // 댓글 식별자
    val userId: String,          // 작성자 식별자
    val userNickname: String,    // 작성자 닉네임
    val userProfileImage: String, // 작성자 프로필 이미지 URL
    val text: String,            // 댓글 내용
    val createdAt: Long,         // 생성 일시 (타임스탬프)
)