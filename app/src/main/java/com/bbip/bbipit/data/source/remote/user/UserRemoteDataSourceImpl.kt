package com.bbip.bbipit.data.source.remote.user

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 정보 관련 원격 데이터 소스 구현체 클래스
 * Firestore 및 Cloud Functions 연동을 통한 유저 프로필 및 상태 데이터 처리 수행
 */
@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseFunctions: FirebaseFunctions,
    private val firebaseMessaging: FirebaseMessaging
) : UserRemoteDataSource {

    /**
     * 알림 푸시 토큰 조회 함수
     */
    override suspend fun getToken(): String? {
        return try {
            // FCM 토큰 발행 및 반환
            val token = firebaseMessaging.token.await()
            Log.d("token", token)
            token
        } catch (e: Exception) {
            // 토큰 발급 실패 예외 처리
            Log.e("token 발급", e.printStackTrace().toString())
            null
        }
    }

    /**
     * 프로필 정보 및 푸시 토큰 갱신 함수
     */
    override suspend fun updateProfile(
        nickname: String?,
        status: String?,
        profileImageUrl: String?,
        fcmToken: String?
    ): String {
        val rawData = hashMapOf(
            "nickname" to nickname,
            "statusMessage" to status,
            "photoURL" to profileImageUrl,
            "fcmToken" to fcmToken
        )
        // 유효한 데이터 항목 필터링 후 원격 서버 전송
        val data = rawData.filterValues { it != null }
        val result = firebaseFunctions.getHttpsCallable("updateProfile").call(data).await()
        val res = result.data as? Map<*, *>
        return res?.get("message")?.toString() ?: "프로필 업데이트 완료"
    }

    /**
     * 온라인 접속 상태 변경 함수
     */
    override suspend fun updateOnlineStatus(isOnline: Boolean): Boolean {
        // 원격 서버 접속 상태 최신화 요청
        val data = hashMapOf("isOnline" to isOnline)
        val result = firebaseFunctions.getHttpsCallable("updateOnlineStatus").call(data).await()
        val res = result.data as Map<*, *>
        return res["success"] as? Boolean ?: false
    }

    /**
     * 타인 프로필 정보 조회 함수
     */
    override suspend fun getUserProfile(targetUid: String): Map<String, Any>? {
        // 특정 유저 식별자 기준 프로필 데이터 획득
        val data = mapOf("targetUid" to targetUid)
        val result = firebaseFunctions.getHttpsCallable("getUserProfile").call(data).await()
        return result.data as? Map<String, Any>
    }

    /**
     * 내 프로필 정보 조회 함수
     */
    override suspend fun getMyProfile(uid: String): Map<String, Any>? {
        return try {
            // 원격 저장소 user 컬렉션 문서 단발성 조회
            val documentSnapshot = firestore.collection("Users")
                .document(uid)
                .get()
                .await()
            documentSnapshot.data
        } catch (e: Exception) {
            // 조회 실패 예외 처리
            null
        }
    }
}