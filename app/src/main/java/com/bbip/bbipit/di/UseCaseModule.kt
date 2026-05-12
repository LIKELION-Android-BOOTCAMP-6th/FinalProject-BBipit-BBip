package com.bbip.bbipit.di

import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.usecase.LoginUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 provides > 인터페이스가 없거나, 외부 라이브러리라 직접 만들어서 줄 때(ex. firebase)

 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideLoginUseCase(
        authRepository: AuthRepository,
        userRepository: UserRepository
    ) = LoginUseCase(authRepository, userRepository)
}