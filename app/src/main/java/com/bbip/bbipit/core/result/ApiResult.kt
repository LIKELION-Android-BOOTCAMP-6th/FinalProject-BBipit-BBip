package com.bbip.bbipit.core.result

import com.bbip.bbipit.domain.error.AppError

/**
 통신 시 크래시 방지 + 에러 일관성을 위함
 도메인/데이터 레이어에서 결과를 전달 시 감싸기

 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Failure(val error: AppError) : ApiResult<Nothing>()
}

/**
 ApiResult를 편리하게 처리하기 위한 확장 함수들입니다.
 */
inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

inline fun <T> ApiResult<T>.onFailure(action: (AppError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) action(error)
    return this
}

/**
 데이터 변환 시 사용
 통신 성공 -> 변환
 실패 -> 에러 메세지 출력
 */
fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> {
    return when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(data))
        is ApiResult.Failure -> ApiResult.Failure(error)
    }
}