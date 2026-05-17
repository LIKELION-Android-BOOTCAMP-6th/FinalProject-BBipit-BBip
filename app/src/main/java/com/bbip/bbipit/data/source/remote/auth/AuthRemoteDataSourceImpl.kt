package com.bbip.bbipit.data.source.remote.auth

import android.content.Context
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 관련 원격 데이터 소스 구현체입니다.
 * Firebase Auth를 사용하여 사용자 인증 처리를 수행합니다.
 */
@Singleton
class AuthRemoteDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) : AuthRemoteDataSource {

    // 카카오 로그인
    override suspend fun loginWithKakao(): String { TODO("Not yet implemented") }
    // 구글 로그인
    override suspend fun loginWithGoogle(idToken: String) { TODO("Not yet implemented") }

    // 커스텀 토큰 로그인
    override suspend fun signInWithCustomToken(accessToken: String) {
        firebaseAuth.signInWithCustomToken(accessToken).await()
    }

    // 이메일 회원가입
    override suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        return firebaseAuth.createUserWithEmailAndPassword(email, password).await()
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
}