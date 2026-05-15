package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**


 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val firebaseAuth: FirebaseAuth
): AuthRepository {
    override suspend fun kakaoLogin() {
        val accessToken = authRemoteDataSource.loginWithKakao()
        authRemoteDataSource.signInWithCustomToken(accessToken)
    }

    override suspend fun signInWithGoogleIdToken(idToken: String) {
        TODO("Not yet implemented")
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        try {
            // 1. Firebase Auth를 통한 회원가입 수행
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user

            // 2. 가입 성공 로그
            Log.d("Auth", "회원가입 성공: ${user?.email} (UID: ${user?.uid})")

        } catch (e: Exception) {
            // 실패 시 에러 처리 (이미 가입된 이메일, 비밀번호 취약 등)
            Log.e("Auth", "회원가입 실패: ${e.message}")
            throw e
        }
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        try {
            // Firebase.auth 대신 주입받은 firebaseAuth 인스턴스 사용
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            // 성공 시 로그 (필요 시)
            Log.d("Auth", "로그인 성공: ${firebaseAuth.currentUser?.uid}")
        } catch (e: Exception) {
            // 실패 시 에러 로그 및 상위 계층으로 예외 전달
            Log.e("Auth", "로그인 실패: ${e.message}")
            throw e
        }
    }

    override fun getCurrentUserUid(): String? {
        return firebaseAuth.currentUser?.uid
    }
}