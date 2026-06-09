package com.bbip.bbipit.data.source.remote.user

import android.net.Uri

/**
 * 사용자 정보 관련 원격 데이터 소스 인터페이스
 */
interface UserRemoteDataSource {

    /**
     * 알림 푸시 토큰 조회 함수
     */
    suspend fun getToken(): String?

    /**
     * 프로필 정보 및 푸시 토큰 갱신 함수
     */
    suspend fun updateProfile(nickname: String?, status: String?, profileImageUrl: String?, fcmToken: String?): String

    /**
     * 온라인 접속 상태 변경 함수
     */
    suspend fun updateOnlineStatus(isOnline: Boolean): Boolean

    /**
     * 타인 프로필 정보 조회 함수
     */
    suspend fun getUserProfile(targetUid: String): Map<String, Any>?

    /**
     * 내 프로필 정보 조회 함수
     */
    suspend fun getMyProfile(uid: String): Map<String, Any>?
    suspend fun getUserOnlineStatus(uid: String): Boolean?

    suspend fun uploadProfileImage(localFileUri: Uri): String
}