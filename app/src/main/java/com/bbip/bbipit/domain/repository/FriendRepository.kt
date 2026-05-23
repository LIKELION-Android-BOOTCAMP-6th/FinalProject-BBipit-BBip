package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.entity.User
import kotlinx.coroutines.flow.StateFlow

/**
 * 친구 데이터 처리 Repository 인터페이스
 */
interface FriendRepository {

    // 친구 목록 Flow
    val myFriends: StateFlow<List<Friend>>

    /**
     * 친구 프로필 및 관계 상태 조회 함수
     */
    suspend fun getFriendProfileWithStatus(targetUid: String): Result<Pair<User, String>>

    /**
     * 친구 요청 수락 함수
     */
    suspend fun acceptFriendRequest(targetUid: String): Result<Boolean>

    /**
     * 친구 요청 거절 함수
     */
    suspend fun declineFriendRequest(targetUid: String): Result<Boolean>

    /**
     * 받은 친구 요청 대기 목록 조회 함수
     */
    suspend fun getPendingFriendRequests(): Result<List<User>>

    /**
     * 친구 요청 전송 함수
     */
    suspend fun sendFriendRequest(targetCode: String): Result<String>

    /**
     * 친구 삭제 함수
     */
    suspend fun deleteFriend(targetUid: String): Result<String>

    /**
     * 수락된 친구 목록 조회 함수
     */
    suspend fun getMyAcceptedFriends(): Result<List<Friend>>

    /**
     * 친구 목록 실시간 구독 함수
     */
    fun startObservingFriends(myUid: String)
}