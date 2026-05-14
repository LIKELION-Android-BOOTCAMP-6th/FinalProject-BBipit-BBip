package com.bbip.bbipit.domain.repository

/**
 소셜/이메일 로그인에 필요한 함수명 선언하는 곳
 구현은 data/repository/AuthRepositoryImpl.kt로
 */
interface AuthRepository {
    suspend fun kakaoLogin()
    suspend fun signInWithGoogleIdToken(idToken: String)
    suspend fun signUpWithEmail(email: String, password: String)
    suspend fun signInWithEmail(email: String, password: String)

    /**
     * 현재 로그인된 사용자 UID 가져오기
     */
    fun getCurrentUserUid(): String?
}