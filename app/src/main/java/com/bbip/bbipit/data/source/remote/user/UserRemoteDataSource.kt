package com.bbip.bbipit.data.source.remote.user

/**
 * 파이어베이스 클라우드 메시징, 인증 프로필 갱신 및 전체 친구 관계 인터페이스 정의 원격 데이터 소스 추상화 인터페이스
 */
interface UserRemoteDataSource {
    suspend fun getToken(): String?
    suspend fun updateProfile(nickname: String?, status: String?, profileImageUrl: String?, fcmToken: String?): String
    suspend fun updateOnlineStatus(isOnline: Boolean): Boolean
    suspend fun getUserProfile(targetUid: String): Map<String, Any>?
    suspend fun getMyProfile(uid: String): Map<String, Any>?
}