package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.User

/**
 * 사용자 관련 데이터 처리를 담당하는 리포지토리입니다.
 * 프로필 관리 및 친구 관계 설정을 수행합니다.
 */
interface UserRepository {
    suspend fun getFcmToken(): String?
    // 프로필 업데이트
    suspend fun updateProfile(nickname: String? = null, status: String? = null, profileImageUrl: String? = null, fcmToken: String? = null): Result<String>

    // 친구 요청 전송
    suspend fun sendFriendRequest(targetUid: String): Result<String>

    // 친구 삭제
    suspend fun deleteFriend(targetUid: String): Result<String>

    // 친구 목록 조회
    suspend fun getMyAcceptedFriends(): Result<List<User>>

    // 하트비트 상태 업데이트
    suspend fun updateHeartbeat(currentRoomId: String?): Result<Unit>

    // 온라인 상태 업데이트
    suspend fun updateOnlineStatus(isOnline: Boolean): Result<Boolean>

    // 친구 요청 수락
    suspend fun acceptFriendRequest(targetUid: String): Result<Boolean>

    // 친구 요청 거절
    suspend fun declineFriendRequest(targetUid: String): Result<Boolean>

    // 유저 프로필 조회 (타인)
    suspend fun getUserProfile(targetUid: String): Result<User>

    // 내 프로필 조회 (자신)
    suspend fun getMyProfile(uid: String): Result<User>

    // 친구 프로필 및 상태 조회
    suspend fun getFriendProfileWithStatus(targetUid: String): Result<Pair<User, String>>
}