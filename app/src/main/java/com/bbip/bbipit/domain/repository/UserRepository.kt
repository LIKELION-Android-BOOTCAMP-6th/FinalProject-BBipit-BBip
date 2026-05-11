package com.bbip.bbipit.domain.repository

/**
 유저 로그인 및 정보 관리
 */

interface UserRepository {

    fun isLogin() = true
    suspend fun updateProfile()
}