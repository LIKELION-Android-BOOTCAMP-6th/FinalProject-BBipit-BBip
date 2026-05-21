package com.bbip.bbipit.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.bbip.bbipit.core.util.AudioRecorder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 파이어베이스 SDK 및 안드로이드 시스템 프레임워크 서비스 객체 인스턴스 생성 경로 정의 의존성 주입 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    // 파이어베이스 인증 시스템 연동 제어 인스턴스 주입 경로 수립
    @Provides
    @Singleton
    fun provideFirebaseAuth() : FirebaseAuth = FirebaseAuth.getInstance()

    // 오디오 파일 보관 처리 전담 파이어베이스 클라우드 스토리지 인스턴스 주입 경로 수립
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    /**
     * 영속성 로컬 오프라인 캐시 동기화 옵션 추가 반영 내장 파이어스토어 데이터베이스 싱글톤 인스턴스 빌드 생성 함수
     */
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val settings = firestoreSettings {
            setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
        }
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = settings
        return firestore
    }

    // 아시아 동북부 3(서울) 리전 호스팅 주소 명시 지정 백엔드 클라우드 함수 호출 인스턴스 빌드 생성 함수
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("asia-northeast3")

    // 기기 간 데이터 알림 푸시 처리 담당 파이어베이스 클라우드 메시징 인스턴스 주입 경로 수립
    @Provides
    @Singleton
    fun provideFirebaseMessaging() : FirebaseMessaging = FirebaseMessaging.getInstance()

    /**
     * 안드로이드 전역 애플리케이션 컨텍스트 주입 기반 GPS 통합 위치 제공 클라이언트 객체 생성 함수
     */
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * 내부 캐시 임시 저장 경로 확보 목적의 컨텍스트 공급 기반 커스텀 음성 녹음 제어 유틸리티 객체 생성 함수
     */
    @Provides
    @Singleton
    fun provideAudioRecorder(
        @ApplicationContext context: Context
    ): AudioRecorder {
        return AudioRecorder(context)
    }
    @Provides
    @Singleton
    fun provideCredentialManager(
        @ApplicationContext context: Context
    ): CredentialManager = CredentialManager.create(context)

}