package com.bbip.bbipit.domain.repository

import android.content.Context
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
    //소셜 로그인 파이어베이스 어스로 연동
    suspend fun signInWithCustomToken(idToken: String, type: LoginType): Result<Unit>
    // 이메일 회원가입
    suspend fun signUpWithEmail(email: String, password: String, nickname: String): Result<AuthResult>

    // 이메일 로그인
    suspend fun signInWithEmail(email: String, password: String): Result<AuthResult>

    // 사용자 UID 조회
    fun getCurrentUserUid(): String?

    // 인증 상태 흐름
    fun getAuthStateFlow(): Flow<String?>

    //약관 내용 불러오기
    suspend fun getTerms(type: TermsType): Result<String>

    fun isAutoLogin() : Boolean
    fun isEmailVerified(): Boolean

    suspend fun signOut(type: LoginType = LoginType.EMAIL)
}