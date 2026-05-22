package com.bbip.bbipit.data.source.remote.user

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 파이어베이스 기능 컴포넌트 통합 호출 기반 사용자 상세 정보 및 원격 소셜 인터랙션 핸들링 구현체 클래스
 */
@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
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
        val data = rawData.filterValues { it != null }
        val result = firebaseFunctions.getHttpsCallable("updateProfile").call(data).await()
        val res = result.data as? Map<*, *>
        return res?.get("message")?.toString() ?: "프로필 업데이트 완료"
    }


    // 온라인 상태 업데이트
    override suspend fun updateOnlineStatus(isOnline: Boolean): Boolean {
        val data = hashMapOf("isOnline" to isOnline)
        val result = firebaseFunctions.getHttpsCallable("updateOnlineStatus").call(data).await()
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