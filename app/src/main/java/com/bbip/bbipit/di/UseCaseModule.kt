package com.bbip.bbipit.di

import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.usecase.LoginUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideLoginUseCase(auth: AuthRepository) = LoginUseCase(auth)
}