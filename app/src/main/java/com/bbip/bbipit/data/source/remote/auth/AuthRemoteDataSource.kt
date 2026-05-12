package com.bbip.bbipit.data.source.remote.auth

/**
 서버와 통신을 위한 함수명만 선언
 */
interface AuthRemoteDataSource {
    suspend fun loginWithKakao(): String
    suspend fun loginWithGoogle(idToken: String)
    suspend fun signInWithCustomToken(accessToken: String)
    suspend fun signUpWithEmail(email: String, password: String)
    suspend fun signInWithEmail(email: String, password: String)
}