package com.bbip.bbipit.data.repository

import com.bbip.bbipit.data.datasource.UserRemoteDataSource
import com.bbip.bbipit.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource
) : UserRepository{
    override suspend fun updateProfile() {
        TODO("Not yet implemented")
    }
}