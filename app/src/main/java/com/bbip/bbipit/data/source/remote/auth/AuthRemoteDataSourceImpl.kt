package com.bbip.bbipit.data.source.remote.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import com.bbip.bbipit.domain.type.LoginType
import com.bbip.bbipit.domain.type.TermsType
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.oAuthCredential
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.flow.flow

/**
 * 인증 관련 원격 데이터 소스 구현체입니다.
 * Firebase Auth를 사용하여 사용자 인증 처리를 수행합니다.
 */
@Singleton
class AuthRemoteDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
) : AuthRemoteDataSource {

    private val TAG = "AuthRemoteDataSourceImpl"

    override fun isAutoLogin(): Boolean = firebaseAuth.currentUser != null

    // 커스텀 토큰 로그인
    override suspend fun signInWithCustomToken(accessToken: String, type: LoginType) {
        when (type) {
            LoginType.KAKAO -> {
                val providerId = "oidc.kakao"
                val credential = oAuthCredential(providerId) { setIdToken(accessToken) }
                firebaseAuth.signInWithCredential(credential).await()
            }

            LoginType.GOOGLE -> {
                Log.d("데이터리모트", "구글 로그인 in 커스텀 토큰")
                val credential = GoogleAuthProvider.getCredential(accessToken, null)
                firebaseAuth.signInWithCredential(credential).await()
            }

            else -> {}
        }
    }

    override suspend fun signOutGoogle() {
        try {
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
        } catch (e: ClearCredentialException) {
            e.printStackTrace()
            Log.e("Google Logout ERROR", e.message.toString())
        }

    }

    override suspend fun signOutKakao() = suspendCancellableCoroutine<Unit> { continuation ->
        UserApiClient.instance.logout { error ->
            if (error != null)
                Log.e("Kakao Logout", error.message.toString())
            continuation.resume(Unit) //카카오 로그아웃은 성공 여부와 상관 없이 무조건 토큰 삭제함
        }
    }

    // 이메일 회원가입
    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        nickname: String
    ): AuthResult {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user

        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(nickname)
            .build()

        user?.updateProfile(profileUpdate)?.await()

        // 업데이트된 닉네임 정보를 서버가 확실하게 인지할 수 있도록 토큰을 리프레시
        firebaseAuth.currentUser?.reload()?.await()

        return authResult
    }

    // 이메일 로그인
    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    // 현재 사용자 ID 반환
    override fun getCurrentUserUid(): String? {
        return firebaseAuth.currentUser?.uid
    }

    // 인증 상태 흐름 관찰
    override fun getAuthStateFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        firebaseAuth.addAuthStateListener(listener)

        // 리스너 해제
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun getTerms(type: TermsType): String = withContext(Dispatchers.IO) {
        URL(type.url).readText()
    }

    override suspend fun reloadCurrentUser() {
        firebaseAuth.currentUser?.reload()?.await()
    }

    override fun isEmailVerified(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        val provider = user.providerData.map { it.providerId }
        return if (provider.contains(LoginType.GOOGLE.provider) || provider.contains(LoginType.KAKAO.provider)) true
        else user.isEmailVerified
    }

    /**
     * 프로필 이미지를 사용자의 UID 폴더 밑에 단 하나만 존재하도록 업로드하는 함수
     * 파일명을 'profile.jpg'로 고정하여 업로드 시 자동으로 덮어쓰기
     */
    override suspend fun uploadProfileImage(myUid: String, localFileUri: Uri): String {
        // ✨ 핵심: 파일명을 고정하여 단 하나의 파일만 유지 (profiles/{uid}/profile.jpg)
        val fileName = "profiles/$myUid/profile.jpg"
        val profileRef = storage.reference.child(fileName)

        return profileRef.putFile(localFileUri).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            profileRef.downloadUrl
        }.await().toString()
    }

    /**
     *  1단계: 유저 데이터 일괄 청소(Firestore, Storage, UserCodes 등)
     * 2단계: Firebase Authentication 콘솔에서 해당 계정 완벽 영구 삭제 일괄 처리
     */
    override suspend fun deleteAccountData(): Unit = withContext(Dispatchers.IO) {
        Log.d(TAG, "deleteAccountData: Cloud Functions 호출 시작")
        try {
            val result = functions
                .getHttpsCallable("deleteAccountData")
                .call()
                .await()

            val data = result.data as? Map<*, *>
            val isSuccess = data?.get("success") as? Boolean ?: false

            if (!isSuccess) {
                throw Exception("서버 데이터 정리 유효성 검증 실패")
            }
        } catch (exception: Exception) {
            Log.e(TAG, "deleteAccountData 통신 예외 발생: ${exception.message}")
            if (exception is FirebaseFunctionsException) {
                throw Exception("서버 트랜잭션 오류 [${exception.code}]: ${exception.message}")
            } else {
                throw exception
            }
        }
    }
}