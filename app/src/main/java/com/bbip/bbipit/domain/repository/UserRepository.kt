package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.domain.entity.User

/**
 유저 로그인 및 정보 관리
 */

interface UserRepository {

    fun isLogin() = false
    suspend fun updateProfile(nickname: String, status: String, profileImageUrl: String)

    /**
     * 친구 요청 보내기
     * @return 성공 시 결과 메시지 등
     */
    suspend fun sendFriendRequest(targetUid: String): String

    /**
     * 친구 삭제
     */
    suspend fun deleteFriend(targetUid: String): String

    /**
     * 수락된 친구 목록 조회
     */
    suspend fun getMyAcceptedFriends(): List<User>

    /**
     * 사용자의 실시간 상태(온라인 여부, 현재 채팅방) 업데이트
     */
    suspend fun updateHeartbeat(currentRoomId: String?)

    /**
     * 온라인/오프라인 상태 명시적 업데이트
     */
    suspend fun updateOnlineStatus(isOnline: Boolean): Boolean

    /**
     * 친구 요청 수락
     */
    suspend fun acceptFriendRequest(targetUid: String): Boolean

    /**
     * 친구 요청 거절/취소
     */
    suspend fun declineFriendRequest(targetUid: String): Boolean
}