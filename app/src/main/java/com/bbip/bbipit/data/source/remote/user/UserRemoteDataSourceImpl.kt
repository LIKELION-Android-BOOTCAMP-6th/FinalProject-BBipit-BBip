package com.bbip.bbipit.data.source.remote.user

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
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
    private val firebaseMessaging: FirebaseMessaging,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : UserRemoteDataSource {

    /**
     * 유저 코드를 기반으로 친구 여부와 상관없이 유저 프로필 및 관계 정보 조회
     */
    override suspend fun getUserProfileByCode(targetCode: String): Map<String, Any>? {
        return try {
            val data = mapOf("targetCode" to targetCode)
            val result = firebaseFunctions.getHttpsCallable("getUserProfileByCode")
                .call(data)
                .await()

            result.data as? Map<String, Any>
        } catch (e: Exception) {
            Log.e("UserRemoteDataSource", "유저 코드로 프로필 조회 실패: ${e.message}")
            null
        }
    }

    /**
     * DB에서 특정 유저의 온라인 상태(is_online)를 직접 조회하는 함수
     */
    override suspend fun getUserOnlineStatus(uid: String): Boolean? {
        return try {
            val documentSnapshot = firestore.collection("Users")
                .document(uid)
                .get()
                .await()

            // "is_online" 필드 값을 Boolean으로 직접 가져옴 (없으면 null 반환)
            documentSnapshot.getBoolean("is_online")
        } catch (e: Exception) {
            Log.e("UserRemoteDataSource", "온라인 상태 조회 실패: ${e.message}")
            null
        }
    }

    /**
     * 프로필 이미지를 사용자의 UID 폴더 밑에 단 하나만 존재하도록 업로드하는 함수
     * 파일명을 'profile.jpg'로 고정하여 업로드 시 자동으로 덮어쓰기
     */
    override suspend fun uploadProfileImage(localFileUri: Uri): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("로그인된 유저 정보가 없습니다.")
        Log.e("유아이디", uid)
        // ✨ 핵심: 파일명을 고정하여 단 하나의 파일만 유지 (profiles/{uid}/profile.jpg)
        val fileName = "profiles/$uid/profile.jpg"
        val profileRef = storage.reference.child(fileName)

        return profileRef.putFile(localFileUri).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            profileRef.downloadUrl
        }.await().toString()
    }

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
        Log.e("프로필 수정 ", "입력값 $nickname, $status, $profileImageUrl")
        val rawData = hashMapOf(
            "nickname" to nickname,
            "status" to status,
            "profile_image_url" to profileImageUrl,
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