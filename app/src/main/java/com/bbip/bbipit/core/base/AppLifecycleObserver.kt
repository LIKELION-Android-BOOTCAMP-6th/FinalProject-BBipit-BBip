package com.bbip.bbipit.core.base

import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 애플리케이션 전역 수명 주기(Lifecycle) 감지 클래스
 * 앱의 포어그라운드 및 백그라운드 전환 상태를 파악하여 포어그라운드 서비스에 상태 변경 신호를 전달할 목적
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {

    /**
     * 앱의 현재 포어그라운드 위치 여부 플래그
     * 무분별한 값 변조 방지를 위해 외부 읽기 전용 및 내부 수정 제한으로 설계
     */
    var isAppInForeground: Boolean = false
        private set

    /**
     * BackgroundListenerService 대상 수명 주기 상태 변경 알림 함수
     * 인텐트(Intent) 발행을 통한 서비스 시작 및 상태 갱신 신호 송신 목적
     */
    private fun triggerServiceUpdate() {
        val intent = Intent(context, BackgroundListenerService::class.java)

        try {
            // 안드로이드 8.0(Oreo, API 26) 이상 백그라운드 서비스 실행 제한 대응을 위한 예외 처리
            context.startService(intent)
        } catch (e: Exception) {
            // 서비스 시작 실패 시에도 기존 가동 중인 서비스가 존재할 경우 후속 워치독(Watchdog) 신호에 의해 상태가 갱신되도록 처리
        }
    }

    /**
     * 앱의 화면 표시 시점(백그라운드 -> 포그라운드 전환) 시스템 호출 콜백 함수
     * 포어그라운드 플래그 활성화(true) 및 백그라운드 서비스 대상 상태 변화 통지 목적
     * 실시간 세션 제어 로직은 결합도 낮추기를 위해 본 옵저버 대신 BackgroundListenerService 또는 개별 화면 수명 주기로 이관
     */
    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        triggerServiceUpdate()
    }

    /**
     * 앱의 화면 이탈 시점(포그라운드 -> 백그라운드 전환) 시스템 호출 콜백 함수
     * 포어그라운드 플래그 비활성화(false) 및 백그라운드 서비스 대상 상태 변화 통지 목적
     */
    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
        triggerServiceUpdate()
    }
}