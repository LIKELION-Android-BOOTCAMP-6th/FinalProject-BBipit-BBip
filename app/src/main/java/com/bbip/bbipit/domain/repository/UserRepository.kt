package com.bbip.bbipit.domain.repository

import android.net.Uri
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.entity.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.collections.emptyList

/**
 * 유저 프로필 및 상태 관리 Repository 인터페이스
 */
interface UserRepository {

    /**
     * 알림 푸시 토큰 조회 함수
     */
    suspend fun getFcmToken(): String?

    /**
     * 유저 프로필 정보 및 푸시 토큰 업데이트 함수
     */
    suspend fun updateProfile(nickname: String? = null, status: String? = null, profileImageUrl: Uri? = null, fcmToken: String? = null, sessionId: String? = null): Result<String>

    /**
     * 온라인 접속 상태 업데이트 함수
     */
//    suspend fun updateOnlineStatus(isOnline: Boolean): Result<Boolean>

    /**
     * 다른 유저의 프로필 정보 조회 함수
     */
    suspend fun getUserProfile(targetUid: String): Result<User>

    /**
     * 내 프로필 상세 정보 조회 함수
     */
    suspend fun getMyProfile(uid: String): Result<User>
    suspend fun getUserOnlineStatus(uid: String): Result<Boolean>
    suspend fun getUserProfileByCode(targetCode: String): Result<User>
}