package com.bbip.bbipit.core.result

import com.bbip.bbipit.domain.error.AppError

/**
 * 통신 시 크래시 방지 및 에러 일관성을 유지하기 위한 결과 래퍼 클래스입니다.
 * 도메인/데이터 레이어에서 비즈니스 로직의 결과를 전달할 때 사용합니다.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
}

/**
 * Result 객체를 편리하게 처리하기 위한 확장 함수들입니다.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onFailure(action: (AppError) -> Unit): Result<T> {
    if (this is Result.Failure) action(error)
    return this
}

/**
 * 데이터 변환 시 사용하는 확장 함수입니다.
 * 성공 시 데이터를 변환하며, 실패 시에는 기존 에러를 그대로 반환합니다.
 */
fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Failure -> Result.Failure(error)
    }
}