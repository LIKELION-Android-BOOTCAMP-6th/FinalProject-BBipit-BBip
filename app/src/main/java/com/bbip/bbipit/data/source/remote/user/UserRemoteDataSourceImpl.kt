package com.bbip.bbipit.data.source.remote.user

import com.bbip.bbipit.domain.entity.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 관련 원격 데이터 소스 구현체입니다.
 * Firestore 및 Cloud Functions를 통해 사용자 프로필 및 친구 기능을 처리합니다.
 */
@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseFunctions: FirebaseFunctions
) : UserRemoteDataSource {

    // 프로필 업데이트
    override suspend fun updateProfile(nickname: String, status: String, profileImageUrl: String): String {
        val data = hashMapOf("nickname" to nickname, "statusMessage" to status, "photoURL" to profileImageUrl)
        val result = firebaseFunctions.getHttpsCallable("updateProfile").call(data).await()
        val res = result.data as? Map<*, *>
        return res?.get("message")?.toString() ?: "프로필 업데이트 완료"
    }

    // 친구 요청 전송
    override suspend fun sendFriendRequest(targetUid: String): String {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("requestFriend").call(data).await()
        val res = result.data as Map<*, *>
        return res["message"]?.toString() ?: "요청 완료"
    }

    // 친구 삭제
    override suspend fun deleteFriend(targetUid: String): String {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("deleteFriend").call(data).await()
        val res = result.data as Map<*, *>
        return res["message"]?.toString() ?: "삭제 완료"
    }

    // 친구 목록 조회
    override suspend fun getMyAcceptedFriends(): List<User> {
        val result = firebaseFunctions.getHttpsCallable("getMyAcceptedFriends").call().await()
        val res = result.data as Map<*, *>
        val friendsList = res["friends"] as? List<Map<String, Any>> ?: emptyList()
        return friendsList.map { map ->
            User(
                id = map["friend_uid"] as? String ?: "",
                nickname = map["nickname"] as? String ?: "",
                profileImageUrl = map["profile_image_url"] as? String ?: "",
                status = map["status"] as? String ?: "",
                isOnline = map["is_online"] as? Boolean ?: false
            )
        }
    }

    // 유저 생명주기 업데이트
    override suspend fun updateHeartbeat(currentRoomId: String?) {
        val data = hashMapOf("isOnline" to true, "currentRoomId" to currentRoomId)
        firebaseFunctions.getHttpsCallable("updateUserStatus").call(data).await()
    }

    // 온라인 상태 업데이트
    override suspend fun updateOnlineStatus(isOnline: Boolean): Boolean {
        val data = hashMapOf("isOnline" to isOnline)
        val result = firebaseFunctions.getHttpsCallable("updateOnlineStatus").call(data).await()
        val res = result.data as Map<*, *>
        return res["success"] as? Boolean ?: false
    }

    // 친구 요청 수락
    override suspend fun acceptFriendRequest(targetUid: String): Boolean {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("acceptFriendRequest").call(data).await()
        val res = result.data as Map<*, *>
        return res["success"] as? Boolean ?: false
    }

    // 친구 요청 거절
    override suspend fun declineFriendRequest(targetUid: String): Boolean {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("declineFriendRequest").call(data).await()
        val res = result.data as Map<*, *>
        return res["success"] as? Boolean ?: false
    }

    // 유저 프로필 조회
    override suspend fun getUserProfile(targetUid: String): Map<String, Any>? {
        val data = mapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("getUserProfile").call(data).await()
        return result.data as? Map<String, Any>
    }

    // 친구 프로필 정보 및 상태 조회
    override suspend fun getFriendProfileWithStatus(targetUid: String): Map<String, Any> {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("getFriendProfileByUid").call(data).await()
        return result.data as Map<String, Any>
    }

    // 내 프로필 조회 구현
    override suspend fun getMyProfile(uid: String): Map<String, Any>? {
        return try {
            val documentSnapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            documentSnapshot.data
        } catch (e: Exception) {
            null
        }
    }
}