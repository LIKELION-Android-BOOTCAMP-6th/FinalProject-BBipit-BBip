package com.bbip.bbipit.data.repository

import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**


 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource
): AuthRepository {
    override suspend fun kakaoLogin() {
        val accessToken = authRemoteDataSource.loginWithKakao()
        authRemoteDataSource.signInWithCustomToken(accessToken)
    }

    override suspend fun signInWithGoogleIdToken(idToken: String) {
        TODO("Not yet implemented")
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        TODO("Not yet implemented")
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        TODO("Not yet implemented")
    }
}