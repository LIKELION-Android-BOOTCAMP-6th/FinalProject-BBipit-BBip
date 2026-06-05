package com.bbip.bbipit.data.source.remote.auth

import android.net.Uri
import com.bbip.bbipit.domain.type.LoginType
import com.bbip.bbipit.domain.type.TermsType
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow

/**
 * 인증 관련 원격 데이터 소스 인터페이스
 */
interface AuthRemoteDataSource {
    suspend fun signInWithCustomToken(accessToken: String, type: LoginType)
    suspend fun signOutGoogle()
    suspend fun signOutKakao()
    suspend fun signUpWithEmail(email: String, password: String, nickname: String): AuthResult
    suspend fun signInWithEmail(email: String, password: String): AuthResult
    fun getCurrentUserUid(): String?
    fun getAuthStateFlow(): Flow<String?>
    suspend fun getTerms(type: TermsType): String
    fun isAutoLogin() : Boolean
    suspend fun reloadCurrentUser()
    fun isEmailVerified(): Boolean
    suspend fun deleteAccountData()
}