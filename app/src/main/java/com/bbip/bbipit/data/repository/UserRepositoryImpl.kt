package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.repository.UserRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions,
) : UserRepository {

    override suspend fun updateProfile(nickname: String, status: String, profileImageUrl: String) {
        val data = hashMapOf(
            "nickname" to nickname,
            "statusMessage" to status,
            "photoURL" to profileImageUrl
        )
        try {
            firebaseFunctions
                .getHttpsCallable("updateProfile")
                .call(data)
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "프로필 업데이트 실패: ${e.message}")
        }
    }

    override suspend fun sendFriendRequest(targetUid: String): String {
        val data = hashMapOf("targetUid" to targetUid)
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("requestFriend")
                .call(data)
                .await()

            val res = result.data as Map<*, *>
            res["message"]?.toString() ?: "요청 완료"
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 요청 실패: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteFriend(targetUid: String): String {
        val data = hashMapOf("targetUid" to targetUid)
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("deleteFriend")
                .call(data)
                .await()

            val res = result.data as Map<*, *>
            res["message"]?.toString() ?: "삭제 완료"
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 삭제 실패: ${e.message}")
            throw e
        }
    }

    override suspend fun getMyAcceptedFriends(): List<User> {
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("getMyAcceptedFriends")
                .call()
                .await()

            val res = result.data as Map<*, *>
            val friendsList = res["friends"] as? List<Map<String, Any>> ?: emptyList()

            friendsList.map { map ->
                User(
                    id = map["friend_uid"] as? String ?: "",
                    nickname = map["nickname"] as? String ?: "",
                    profileImageUrl = map["profile_image_url"] as? String ?: "",
                    status = map["status"] as? String ?: "",
                    isOnline = map["is_online"] as? Boolean ?: false
                )
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 목록 조회 실패: ${e.message}")
            throw e
        }
    }

    override suspend fun updateHeartbeat(currentRoomId: String?) {
        val data = hashMapOf(
            "isOnline" to true,
            "currentRoomId" to currentRoomId
        )
        try {
            firebaseFunctions
                .getHttpsCallable("updateUserStatus")
                .call(data)
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Heartbeat 업데이트 실패: ${e.message}")
        }
    }

    override suspend fun updateOnlineStatus(isOnline: Boolean): Boolean {
        val data = hashMapOf("isOnline" to isOnline)
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("updateOnlineStatus")
                .call(data)
                .await()
            val res = result.data as Map<*, *>
            res["success"] as? Boolean ?: false
        } catch (e: Exception) {
            Log.e("UserRepository", "온라인 상태 업데이트 실패: ${e.message}")
            false
        }
    }

    override suspend fun acceptFriendRequest(targetUid: String): Boolean {
        val data = hashMapOf("targetUid" to targetUid)
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("acceptFriendRequest")
                .call(data)
                .await()
            val res = result.data as Map<*, *>
            res["success"] as? Boolean ?: false
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 수락 실패: ${e.message}")
            false
        }
    }

    override suspend fun declineFriendRequest(targetUid: String): Boolean {
        val data = hashMapOf("targetUid" to targetUid)
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("declineFriendRequest")
                .call(data)
                .await()
            val res = result.data as Map<*, *>
            res["success"] as? Boolean ?: false
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 거절 실패: ${e.message}")
            false
        }
    }
}
