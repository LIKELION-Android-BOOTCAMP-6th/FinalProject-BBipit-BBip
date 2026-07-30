package com.bbip.bbipit.data.source.remote.friend

import android.content.Context
import com.bbip.bbipit.data.mapper.toDomain
import com.bbip.bbipit.data.mapper.toFriendshipDto
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val firebaseFunctions: FirebaseFunctions
) : FriendRemoteDataSource {

    /**
     * 특정 유저와의 쌍방 친밀도 수락 관계 문맥 및 기초 프로필 맵 구조 에셋 데이터 통합 파싱 일괄 인출 함수
     */
    override suspend fun getFriendProfileWithStatus(targetUid: String): Map<String, Any> {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("getFriendProfileByUid").call(data).await()
        return result.data as Map<String, Any>
    }

    /**
     * 수신 대기 테이블 적재 상대방 친구 요청 대상 반려 거절 의사 플래그 전송 연산 함수
     */
    override suspend fun declineFriendRequest(targetUid: String): Boolean {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("declineFriendRequest").call(data).await()
        val res = result.data as Map<*, *>
        return res["success"] as? Boolean ?: false
    }

    /**
     * 타인 인입 대기 상태 친구 추가 요청 대상 명시적 수락 플래그 원격 반영 함수
     */
    override suspend fun acceptFriendRequest(targetUid: String): Boolean {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("acceptFriendRequest").call(data).await()
        val res = result.data as Map<*, *>
        return res["success"] as? Boolean ?: false
    }

    // 신청 목록 조회
    override suspend fun getPendingFriendRequests(): List<User> {
        val uid = auth.currentUser?.uid ?: throw Exception("로그인이 필요합니다.")

        // Firestore 쿼리: 내가 받은 요청('requested') 상태인 것만 가져오기
        val snapshot = db.collection("Users")
            .document(uid)
            .collection("Friendships")
            .whereEqualTo("friendship_status", "requested")
            .get()
            .await()

        return snapshot.documents.map { doc ->
            val data = doc.data ?: emptyMap<String, Any>()
            User(
                id = doc.id, // 문서 ID가 친구의 UID라고 가정
                nickname = data["nickname"] as? String ?: "",
                profileImageUrl = data["profile_image_url"] as? String ?: "",
                status = data["status"] as? String ?: "",
                isOnline = data["is_online"] as? Boolean ?: false
            )
        }
    }

    /**
     * 대인 관계망 확장 목적 상대방 고유 UID 매개변수 포함 HTTPS 백엔드 서버 대상 친구 추가 요청 이벤트 발신 함수
     */
    override suspend fun sendFriendRequest(targetCode: String): String {
        val data = hashMapOf("targetCode" to targetCode)
        val result = firebaseFunctions.getHttpsCallable("requestFriend").call(data).await()
        val res = result.data as Map<*, *>
        return res["message"]?.toString() ?: "요청 완료"
    }

    /**
     * 연동 해제 대상 상대방 식별자 파라미터 대입 기반 서버 공간 내 쌍방 친구 관계 전면 철회 파기 함수
     */
    override suspend fun deleteFriend(targetUid: String): String {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("deleteFriend").call(data).await()
        val res = result.data as Map<*, *>
        return res["message"]?.toString() ?: "삭제 완료"
    }

    /**
     * 백엔드 엔진 서버 내부 연산 결과 수신 기반 상호 수락 확정 내 인접 친구 프로필 목록 배열 일괄 도출 함수
     * 수신 원시 데이터 리스트 객체의 전용 Friendship 매퍼 확장 함수 구조 이용 도메인 엔티티 유형 배열 변환 가공 목적
     */
    override suspend fun getMyAcceptedFriends(): List<Friend> {
        val result = firebaseFunctions.getHttpsCallable("getMyAcceptedFriends").call().await()
        val res = result.data as Map<*, *>
        val friendsMapList = res["friends"] as? List<Map<String, Any>> ?: emptyList()
        return friendsMapList.map { it.toFriendshipDto().toDomain() }
    }
}