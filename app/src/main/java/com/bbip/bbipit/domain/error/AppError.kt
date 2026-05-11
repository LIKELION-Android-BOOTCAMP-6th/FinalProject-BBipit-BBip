package com.bbip.bbipit.domain.error

sealed class AppError : Throwable() {
    data class Network(override val message: String = "네트워크 연결이 원활하지 않습니다.") : AppError()
    data class Auth(override val message: String = "인증에 실패했습니다. 다시 로그인해주세요.") : AppError()
    data class Server(override val message: String = "서버에 오류가 발생했습니다.") : AppError()
    data class Unknown(override val message: String = "알 수 없는 오류가 발생했습니다.") : AppError()
    data class Upload(override val message: String = "무전 전송을 실패했습니다. 다시 시도해주세요.") : AppError()
    data class Custom(override val message: String) : AppError()
}