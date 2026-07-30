package com.bbip.bbipit.data.di

import com.bbip.bbipit.data.repository.AuthRepositoryImpl
import com.bbip.bbipit.data.repository.ChatRepositoryImpl
import com.bbip.bbipit.data.repository.FriendRepositoryImpl
import com.bbip.bbipit.data.repository.HistoryRepositoryImpl
import com.bbip.bbipit.data.repository.LiveStatusRepositoryImpl
import com.bbip.bbipit.data.repository.NotificationRepositoryImpl
import com.bbip.bbipit.data.repository.UserRepositoryImpl
import com.bbip.bbipit.data.repository.VoiceRepositoryImpl
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.chat.ChatRemoteDataSource
import com.bbip.bbipit.data.source.remote.chat.ChatRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.friend.FriendRemoteDataSource
import com.bbip.bbipit.data.source.remote.friend.FriendRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.history.HistoryRemoteDataSource
import com.bbip.bbipit.data.source.remote.history.HistoryRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.live.LiveStatusRemoteDataSource
import com.bbip.bbipit.data.source.remote.live.LiveStatusRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSource
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.user.UserRemoteDataSource
import com.bbip.bbipit.data.source.remote.user.UserRemoteDataSourceImpl
import com.bbip.bbipit.data.source.remote.voice.VoiceRemoteDataSource
import com.bbip.bbipit.data.source.remote.voice.VoiceRemoteDataSourceImpl
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.ChatRepository
import com.bbip.bbipit.domain.repository.FriendRepository
import com.bbip.bbipit.domain.repository.HistoryRepository
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 인터페이스 규격 및 실제 비즈니스 연산 구현체 클래스 결합 기반 의존성 주입 그래프 형성 추상 힐트(Hilt) 모듈
 * 애플리케이션 전반의 고유 메모리 주소 공유를 위한 싱글톤 컴포넌트 수명 주기 바인딩 처리 목적
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    // ==========================================
    // Remote Data Source Bindings
    // ==========================================

    /**
     * 실시간 접속 상태 및 위경도 통신 전용 원격 데이터 소스 인터페이스와 실제 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindLiveStatusRemoteDataSource(
        liveStatusRemoteDataSourceImpl: LiveStatusRemoteDataSourceImpl
    ): LiveStatusRemoteDataSource

    /**
     * 파이어베이스 계정 인증 관련 원격 데이터 소스 인터페이스와 실제 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        impl: AuthRemoteDataSourceImpl
    ): AuthRemoteDataSource

    /**
     * 유저 정보 관리 및 친구 관계망 제어 전용 원격 데이터 소스 인터페이스와 실제 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(
        impl: UserRemoteDataSourceImpl
    ): UserRemoteDataSource

    /**
     * 실시간 텍스트 채팅 데이터 연동 전용 원격 데이터 소스 인터페이스와 실제 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindChatRemoteDataSource(
        impl: ChatRemoteDataSourceImpl
    ): ChatRemoteDataSource

    /**
     * 무전 음성 메시지 파일 처리 전용 원격 데이터 소스 인터페이스와 실제 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindVoiceRemoteDataSource(
        impl: VoiceRemoteDataSourceImpl
    ): VoiceRemoteDataSource

    /**
     * 시스템 수신 알림 명세 제어 전용 원격 데이터 소스 인터페이스와 실제 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindNotificationRemoteDataSource(
        impl: NotificationRemoteDataSourceImpl
    ): NotificationRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindFriendRemoteDataSource(
        impl: FriendRemoteDataSourceImpl
    ): FriendRemoteDataSource


    // ==========================================
    // Repository Bindings
    // ==========================================

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        impl: HistoryRepositoryImpl
    ): HistoryRepository

    /**
     * 실시간 동기화 상태 엔티티 관리 전담 도메인 계층 리포지토리 인터페이스 및 데이터 계층 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindLiveStatusRepository(
        impl: LiveStatusRepositoryImpl
    ): LiveStatusRepository

    /**
     * 인증 수립 및 토큰 처리 전담 도메인 계층 리포지토리 인터페이스 및 데이터 계층 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    /**
     * 유저 관계 조율 및 정보 조회 전담 도메인 계층 리포지토리 인터페이스 및 데이터 계층 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    /**
     * 채팅 히스토리 관리 전담 도메인 계층 리포지토리 인터페이스 및 데이터 계층 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    /**
     * 실시간 무전 패킷 동기화 전담 도메인 계층 리포지토리 인터페이스 및 데이터 계층 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindVoiceRepository(
        impl: VoiceRepositoryImpl
    ): VoiceRepository

    /**
     * 알림 기록 내역 통제 전담 도메인 계층 리포지토리 인터페이스 및 데이터 계층 구현체 결합
     */
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindFriendRepository(
        impl: FriendRepositoryImpl
    ): FriendRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRemoteDataSource(
        impl: HistoryRemoteDataSourceImpl
    ): HistoryRemoteDataSource
}