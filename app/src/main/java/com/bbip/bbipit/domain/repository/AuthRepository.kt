package com.bbip.bbipit.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 사용자 인증 데이터 처리를 위한 리포지토리 인터페이스
 * 소셜 및 이메일 로그인, 회원가입, 사용자 정보 조회, 인증 상태 감시 기능 정의
 */
interface AuthRepository {
    // 카카오 로그인 수행
    suspend fun kakaoLogin()
    // 구글 ID 토큰을 활용한 로그인 수행
    suspend fun signInWithGoogleIdToken(idToken: String)
    // 이메일 기반 회원가입 수행
    suspend fun signUpWithEmail(email: String, password: String)
    // 이메일 기반 로그인 수행
    suspend fun signInWithEmail(email: String, password: String)
    // 현재 로그인된 사용자 UID 조회
    fun getCurrentUserUid(): String?
    // 인증 상태 변화 감지 스트림 제공
    fun getAuthStateFlow(): Flow<String?>
}