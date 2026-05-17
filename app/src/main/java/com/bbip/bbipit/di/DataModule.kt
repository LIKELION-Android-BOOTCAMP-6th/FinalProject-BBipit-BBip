package com.bbip.bbipit.data.di

import com.bbip.bbipit.data.repository.AuthRepositoryImpl
import com.bbip.bbipit.data.repository.ChatRepositoryImpl
import com.bbip.bbipit.data.repository.NotificationRepositoryImpl
import com.bbip.bbipit.data.repository.UserRepositoryImpl
import com.bbip.bbipit.data.repository.VoiceRepositoryImpl
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.chat.ChatRemoteDataSource
import com.bbip.bbipit.data.source.remote.chat.ChatRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSource
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.user.UserRemoteDataSource
import com.bbip.bbipit.data.source.remote.user.UserRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.voice.VoiceRemoteDataSource
import com.bbip.bbipit.data.source.remote.voice.VoiceRemoteDataSourceImpl
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.ChatRepository
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    // ==========================================
    // Remote Data Source Bindings
    // ==========================================

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        impl: AuthRemoteDataSourceImpl
    ): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(
        impl: UserRemoteDataSourceImpl
    ): UserRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindChatRemoteDataSource(
        impl: ChatRemoteDataSourceImpl
    ): ChatRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindVoiceRemoteDataSource(
        impl: VoiceRemoteDataSourceImpl
    ): VoiceRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindNotificationRemoteDataSource(
        impl: NotificationRemoteDataSourceImpl
    ): NotificationRemoteDataSource


    // ==========================================
    // Repository Bindings
    // ==========================================

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindVoiceRepository(
        impl: VoiceRepositoryImpl
    ): VoiceRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository
}