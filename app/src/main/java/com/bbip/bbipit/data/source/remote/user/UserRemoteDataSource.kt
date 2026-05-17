package com.bbip.bbipit.data.source.remote.user

import com.bbip.bbipit.domain.entity.User

interface UserRemoteDataSource {
    suspend fun updateProfile(nickname: String, status: String, profileImageUrl: String): String
    suspend fun sendFriendRequest(targetUid: String): String
    suspend fun deleteFriend(targetUid: String): String
    suspend fun getMyAcceptedFriends(): List<User>
    suspend fun updateHeartbeat(currentRoomId: String?)
    suspend fun updateOnlineStatus(isOnline: Boolean): Boolean
    suspend fun acceptFriendRequest(targetUid: String): Boolean
    suspend fun declineFriendRequest(targetUid: String): Boolean
    suspend fun getUserProfile(targetUid: String): Map<String, Any>?
    suspend fun getFriendProfileWithStatus(targetUid: String): Map<String, Any>
}