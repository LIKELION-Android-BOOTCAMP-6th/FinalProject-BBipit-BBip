package com.bbip.bbipit.data.source.remote.friend

import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.entity.User

/**
 친구 관련
 */
interface FriendRemoteDataSource {
 suspend fun getFriendProfileWithStatus(targetUid: String): Map<String, Any>
 suspend fun acceptFriendRequest(targetUid: String): Boolean
 suspend fun declineFriendRequest(targetUid: String): Boolean
 suspend fun getPendingFriendRequests(): List<User>
 suspend fun getMyAcceptedFriends(): List<Friend>
 suspend fun sendFriendRequest(targetCode: String): String
 suspend fun deleteFriend(targetUid: String): String
}