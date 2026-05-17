package com.bbip.bbipit.domain.usecase

import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.core.result.Result

/**
 여기서 구현한 걸 뷰모델에서 활용

 */

class LoginUseCase (
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend fun google(idToken: String): Result<Unit> = runCatching {
        authRepository.signInWithGoogleIdToken(idToken)
        //프로필 연동
    }

    suspend fun kakao(): Result<Unit> = runCatching {
        authRepository.kakaoLogin()
    }

    suspend fun loginWithEmail(email: String, password: String): Result<Unit> = runCatching {
        authRepository.signInWithEmail(email, password)
    }

    private suspend fun <T> runCatching(action: suspend () -> T): Result<T> {
        return try {
            Result.Success(action())
        } catch (e: Exception) {
            Result.Failure(if (e is AppError) e else AppError.Custom(e.message ?: "로그인 실패"))
        }
    }
}