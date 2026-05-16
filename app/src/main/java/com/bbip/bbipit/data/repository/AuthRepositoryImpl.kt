package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 관련 데이터 처리를 담당하는 구현체
 * 외부 인증 소스 및 Firebase Auth를 활용한 회원가입, 로그인, 상태 관리 수행
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val firebaseAuth: FirebaseAuth
): AuthRepository {

    // 카카오 로그인 수행
    override suspend fun kakaoLogin() {
        val accessToken = authRemoteDataSource.loginWithKakao()
        authRemoteDataSource.signInWithCustomToken(accessToken)
    }

    // 구글 ID 토큰을 이용한 로그인 구현 예정
    override suspend fun signInWithGoogleIdToken(idToken: String) {
        TODO("Not yet implemented")
    }

    // 이메일 기반 회원가입 수행 및 결과 로그 기록
    override suspend fun signUpWithEmail(email: String, password: String) {
        try {
            // Firebase Auth를 이용한 이메일 회원가입 실행
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            Log.d("Auth", "회원가입 성공: ${user?.email} (UID: ${user?.uid})")
        } catch (e: Exception) {
            // 회원가입 실패 시 에러 로그 기록 및 예외 상위 전파
            Log.e("Auth", "회원가입 실패: ${e.message}")
            throw e
        }
    }

    // 이메일 기반 로그인 수행 및 결과 로그 기록
    override suspend fun signInWithEmail(email: String, password: String) {
        try {
            // 주입된 FirebaseAuth 인스턴스를 통한 로그인 실행
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Log.d("Auth", "로그인 성공: ${firebaseAuth.currentUser?.uid}")
        } catch (e: Exception) {
            // 로그인 실패 시 에러 로그 기록 및 예외 상위 전파
            Log.e("Auth", "로그인 실패: ${e.message}")
            throw e
        }
    }

    // 현재 로그인된 사용자의 UID 반환
    override fun getCurrentUserUid(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * 인증 상태 변화 감지 및 스트림 발행
     * 로그인 상태 변화에 따른 사용자 UID 발행 및 로그아웃 시 null 처리
     */
    override fun getAuthStateFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }
}