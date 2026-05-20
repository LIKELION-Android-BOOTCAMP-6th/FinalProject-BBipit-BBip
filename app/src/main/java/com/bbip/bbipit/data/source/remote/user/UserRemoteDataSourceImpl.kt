package com.bbip.bbipit.data.source.remote.user

import android.util.Log
import com.bbip.bbipit.data.mapper.toDomain
import com.bbip.bbipit.data.mapper.toFriendshipDto
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.entity.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 파이어베이스 기능 컴포넌트 통합 호출 기반 사용자 상세 정보 및 원격 소셜 인터랙션 핸들링 구현체 클래스
 */
@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val firebaseFunctions: FirebaseFunctions,
    private val firebaseMessaging: FirebaseMessaging
) : UserRemoteDataSource {

    /**
     * 구글 푸시 서버 환경 기준 현재 단말기 바인딩 가용 고유 푸시 FCM 토큰 문자열 비동기 발행 인출 함수
     */
    override suspend fun getToken(): String? {
        return try {
            val token = firebaseMessaging.token.await()
            Log.d("token", token)
            token
        } catch (e: Exception) {
            Log.e("token 발급", e.printStackTrace().toString())
            null
        }
    }

    /**
     * 닉네임, 상태메시지, 사진 주소 및 토큰 정보 매핑 후 HTTPS 클라우드 함수 원격 게이트웨이 트리거 기반 내 프로필 변경 함수
     * 페일로드 전송용 널(Null) 값 제외 유효 엔트리 데이터 항목 스크리닝 필터링 처리 포함
     */
    override suspend fun updateProfile(nickname: String?, status: String?, profileImageUrl: String?, fcmToken: String?): String {
        val rawData = hashMapOf("nickname" to nickname, "statusMessage" to status, "photoURL" to profileImageUrl, "fcmToken" to fcmToken)
        val data = rawData.filterValues { it != null }
        val result = firebaseFunctions.getHttpsCallable("updateProfile").call(data).await()
        val res = result.data as? Map<*, *>
        return res?.get("message")?.toString() ?: "프로필 업데이트 완료"
    }

    /**
     * 대인 관계망 확장 목적 상대방 고유 UID 매개변수 포함 HTTPS 백엔드 서버 대상 친구 추가 요청 이벤트 발신 함수
     */
    override suspend fun sendFriendRequest(targetUid: String): String {
        val data = hashMapOf("targetUid" to targetUid)
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
            // Firestore 데이터를 User 객체로 변환
            doc.toObject(User::class.java) ?: throw Exception("데이터 변환 실패")
        }
    }

    // 온라인 상태 업데이트
    override suspend fun updateOnlineStatus(isOnline: Boolean): Boolean {
        val data = hashMapOf("isOnline" to isOnline)
        val result = firebaseFunctions.getHttpsCallable("updateOnlineStatus").call(data).await()
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
     * 특정 사용자 UID 코드 키 조건 기준 서버 저장소 원격 스캔을 통한 기초 프로필 원본 매핑 데이터 단발성 인출 함수
     */
    override suspend fun getUserProfile(targetUid: String): Map<String, Any>? {
        val data = mapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("getUserProfile").call(data).await()
        return result.data as? Map<String, Any>
    }

    /**
     * 특정 유저와의 쌍방 친밀도 수락 관계 문맥 및 기초 프로필 맵 구조 에셋 데이터 통합 파싱 일괄 인출 함수
     */
    override suspend fun getFriendProfileWithStatus(targetUid: String): Map<String, Any> {
        val data = hashMapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("getFriendProfileByUid").call(data).await()
        return result.data as Map<String, Any>
    }

    /**
     * 내 고유 식별 명세 인자 대입 기반 파이어스토어 영속성 루트 users 컬렉션 영역 내 개인 원본 프로필 스냅샷 정보 다이렉트 조회 함수
     */
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