package com.bbip.bbipit.data.source.remote.auth

import android.content.Context
import com.bbip.bbipit.domain.type.LoginType
import com.bbip.bbipit.presentation.auth.ui.TermsType
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow

interface AuthRemoteDataSource {
    suspend fun loginWithKakao(): String
    suspend fun loginWithGoogle(appContext: Context): String?
    suspend fun signInWithCustomToken(accessToken: String, type: LoginType)
    suspend fun signOutGoogle()
    suspend fun signOutKakao()
    suspend fun signUpWithEmail(email: String, password: String, nickname: String): AuthResult
    suspend fun signInWithEmail(email: String, password: String): AuthResult

    fun getCurrentUserUid(): String?
    fun getAuthStateFlow(): Flow<String?>

    suspend fun getTerms(type: TermsType): String
    fun isAutoLogin() : Boolean

    // 유저의 최신 인증 상태(이메일 인증 여부 등)를 반영하기 위해 리프레시
    suspend fun reloadCurrentUser()

    // 유저가 이메일 인증되었는지 여부 반환
    fun isEmailVerified(): Boolean
}