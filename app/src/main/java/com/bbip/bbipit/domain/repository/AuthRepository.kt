package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.presentation.auth.ui.TermsType
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 인증 데이터 처리를 담당하는 리포지토리입니다.
 * 로그인, 회원가입 및 사용자 인증 상태 관리를 수행합니다.
 */
interface AuthRepository {
    // 카카오 로그인
    suspend fun kakaoLogin(): Result<Unit>

    // 구글 로그인
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>

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
}