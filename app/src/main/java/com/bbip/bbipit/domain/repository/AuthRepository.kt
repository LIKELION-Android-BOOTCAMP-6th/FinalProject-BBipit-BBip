package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.type.LoginType
import com.bbip.bbipit.domain.type.TermsType
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 인증 데이터 처리를 담당하는 리포지토리입니다.
 * 로그인, 회원가입 및 사용자 인증 상태 관리를 수행합니다.
 */
interface AuthRepository {

    /**
     * 소셜 로그인 연동 함수
     */
    suspend fun signInWithCustomToken(idToken: String, type: LoginType): Result<Unit>

    /**
     * 이메일 회원가입 함수
     */
    suspend fun signUpWithEmail(email: String, password: String, nickname: String): Result<AuthResult>

    /**
     * 이메일 로그인 함수
     */
    suspend fun signInWithEmail(email: String, password: String): Result<AuthResult>

    /**
     * 현재 유저 UID 조회 함수
     */
    fun getCurrentUserUid(): String?

    /**
     * 인증 상태 변경 관찰 Flow 생성 함수
     */
    fun getAuthStateFlow(): Flow<String?>

    /**
     * 약관 내용 조회 함수
     */
    suspend fun getTerms(type: TermsType): Result<String>

    /**
     * 자동 로그인 여부 확인 함수
     */
    fun isAutoLogin() : Boolean

    /**
     * 이메일 인증 완료 여부 확인 함수
     */
    fun isEmailVerified(): Boolean

    /**
     * 로그아웃 함수
     */
    suspend fun signOut(type: LoginType = LoginType.EMAIL, isDuplicated: Boolean = false)

    suspend fun deleteAccount(type: LoginType = LoginType.EMAIL, token: String): Result<Unit>
    suspend fun logoutServerCleanup(): Result<Unit>

    fun saveSessionId(id: String)
    fun getLocalSessionId(): String?
}