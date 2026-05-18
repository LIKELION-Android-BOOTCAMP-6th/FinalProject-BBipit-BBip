package com.bbip.bbipit.data.source.remote.auth

import com.bbip.bbipit.presentation.auth.ui.TermsType
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow

interface AuthRemoteDataSource {
    suspend fun loginWithKakao(): String
    suspend fun loginWithGoogle(idToken: String)
    suspend fun signInWithCustomToken(accessToken: String)

    suspend fun signUpWithEmail(email: String, password: String, nickname: String): AuthResult
    suspend fun signInWithEmail(email: String, password: String): AuthResult

    fun getCurrentUserUid(): String?
    fun getAuthStateFlow(): Flow<String?>

    suspend fun getTerms(type: TermsType): String
    fun isAutoLogin() : Boolean
}