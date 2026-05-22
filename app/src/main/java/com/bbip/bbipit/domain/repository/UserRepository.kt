package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.entity.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.collections.emptyList

/**
 * 프로필 관리 및 친구 관계 설정 명시 기반 유저 데이터 처리 추상화 도메인 계층 리포지토리 인터페이스
 */
interface UserRepository {

    suspend fun getFcmToken(): String?

    suspend fun updateProfile(nickname: String? = null, status: String? = null, profileImageUrl: String? = null, fcmToken: String? = null): Result<String>

    suspend fun updateHeartbeat(currentRoomId: String?): Result<Unit>

    suspend fun updateOnlineStatus(isOnline: Boolean): Result<Boolean>

    suspend fun getUserProfile(targetUid: String): Result<User>

    suspend fun getMyProfile(uid: String): Result<User>
}