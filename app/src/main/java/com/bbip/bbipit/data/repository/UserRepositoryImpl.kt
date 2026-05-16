package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.data.mapper.toDomain
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.repository.UserRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 정보 데이터 처리 구현체
 * Firebase Functions를 활용한 사용자 프로필 업데이트, 친구 요청 및 관리 기능 수행
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions,
) : UserRepository {
    // 사용자 프로필 업데이트 처리
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
    // 친구 요청 전송 처리
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
    // 친구 삭제 처리
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
    // 승인된 친구 목록 조회 처리
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
    // 사용자 온라인 상태 및 접속 중인 방 정보 업데이트
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
    // 온라인 상태 전환 처리
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
    // 친구 요청 수락 처리
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
    // 친구 요청 거절 처리
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
    // 사용자 프로필 정보 조회 처리
    override suspend fun getUserProfile(targetUid: String): Result<User> {
        return try {
            val data = mapOf("targetUid" to targetUid)
            // Cloud Functions 통한 사용자 정보 호출
            val result = firebaseFunctions
                .getHttpsCallable("getUserProfile")
                .call(data)
                .await()
            // 결과 데이터 파싱
            val response = result.data as? Map<String, Any>
            val success = response?.get("success") as? Boolean ?: false
            val profileMap = response?.get("profile") as? Map<String, Any>
            if (success && profileMap != null) {
                Result.success(profileMap.toDomain())
            } else {
                Result.failure(Exception("유저 정보를 찾을 수 없습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // 친구 프로필 정보 및 관계 상태 조회 처리
    override suspend fun getFriendProfileWithStatus(targetUid: String): Result<Pair<User, String>> {
        return try {
            val data = hashMapOf("targetUid" to targetUid)
            val result = firebaseFunctions
                .getHttpsCallable("getFriendProfileByUid")
                .call(data)
                .await()
            val response = result.data as Map<String, Any>
            // 사용자 정보 변환
            val profileMap = response["profile"] as? Map<String, Any> ?: throw Exception("친구 정보를 찾을 수 없습니다.")
            val user = profileMap.toDomain()
            // 친구 관계 상태 추출
            val friendshipStatus = response["friendship_status"] as? String ?: "none"
            Result.success(Pair(user, friendshipStatus))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
