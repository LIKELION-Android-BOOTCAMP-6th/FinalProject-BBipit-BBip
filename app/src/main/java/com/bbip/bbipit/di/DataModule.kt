package com.bbip.bbipit.di

import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.user.UserRemoteDataSource
import com.bbip.bbipit.data.source.remote.user.UserRemoteDataSourceImpl
import com.bbip.bbipit.data.repository.AuthRepositoryImpl
import com.bbip.bbipit.data.repository.NotiRepositoryImpl
import com.bbip.bbipit.data.repository.UserRepositoryImpl
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.NotiRepository
import com.bbip.bbipit.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 bind > 인터페이스와 구현체 연결
 도메인의 레포지토리와 data의 레포지토리Impl 주입

 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /**
     레포지토리
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    /**
     데이터소스
     */
    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(impl: AuthRemoteDataSourceImpl): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(impl: UserRemoteDataSourceImpl) : UserRemoteDataSource
}