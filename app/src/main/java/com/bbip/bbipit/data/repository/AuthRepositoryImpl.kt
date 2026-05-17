package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.AuthRepository
import com.google.firebase.auth.AuthResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 관련 데이터 처리를 담당하는 구현체입니다.
 * Firebase 및 외부 인증 서비스와의 상호작용을 관리합니다.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource
): AuthRepository {

    // 카카오 로그인 수행
    override suspend fun kakaoLogin(): Result<Unit> {
        return try {
            val accessToken = authRemoteDataSource.loginWithKakao()
            authRemoteDataSource.signInWithCustomToken(accessToken)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("Auth", "카카오 로그인 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "카카오 로그인 중 오류 발생"))
        }
    }

    // 구글 ID 토큰을 이용한 로그인 수행
    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        return try {
            authRemoteDataSource.loginWithGoogle(idToken)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("Auth", "구글 로그인 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "구글 로그인 중 오류 발생"))
        }
    }

    // 이메일 기반 회원가입 수행
    override suspend fun signUpWithEmail(email: String, password: String): Result<AuthResult> {
        return try {
            val authResult = authRemoteDataSource.signUpWithEmail(email, password)
            val user = authResult.user
            Log.d("Auth", "회원가입 성공: ${user?.email} (UID: ${user?.uid})")
            Result.Success(authResult)
        } catch (e: Exception) {
            Log.e("Auth", "회원가입 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "회원가입 중 오류 발생"))
        }
    }

    // 이메일 기반 로그인 수행
    override suspend fun signInWithEmail(email: String, password: String): Result<AuthResult> {
        return try {
            val authResult = authRemoteDataSource.signInWithEmail(email, password)
            Log.d("Auth", "로그인 성공: ${authResult.user?.uid}")
            Result.Success(authResult)
        } catch (e: Exception) {
            Log.e("Auth", "로그인 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "로그인 중 오류 발생"))
        }
    }

    // 현재 사용자 UID 반환
    override fun getCurrentUserUid(): String? {
        return authRemoteDataSource.getCurrentUserUid()
    }

    // 인증 상태 변경 흐름 반환
    override fun getAuthStateFlow(): kotlinx.coroutines.flow.Flow<String?> {
        return authRemoteDataSource.getAuthStateFlow()
    }
}