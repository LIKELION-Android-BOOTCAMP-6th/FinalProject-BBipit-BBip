package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.entity.User
import kotlinx.coroutines.flow.StateFlow

interface FriendRepository {
    val myFriends: StateFlow<List<Friend>>
    suspend fun getFriendProfileWithStatus(targetUid: String): Result<Pair<User, String>>

    suspend fun acceptFriendRequest(targetUid: String): Result<Boolean>

    suspend fun declineFriendRequest(targetUid: String): Result<Boolean>

    suspend fun getPendingFriendRequests(): Result<List<User>>

    suspend fun sendFriendRequest(targetCode: String): Result<String>

    suspend fun deleteFriend(targetUid: String): Result<String>

    suspend fun getMyAcceptedFriends(): Result<List<Friend>>

    fun startObservingFriends(myUid: String)
}