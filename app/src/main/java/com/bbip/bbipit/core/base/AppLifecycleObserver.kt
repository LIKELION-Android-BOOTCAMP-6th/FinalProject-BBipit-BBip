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
 * 앱의 포어그라운드/백그라운드 전환 상태 파악 및 서비스로의 상태 변경 신호 전달
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {

    // 앱의 현재 포어그라운드 위치 여부 플래그
    var isAppInForeground: Boolean = false
        private set

    /**
     * 서비스 대상 수명 주기 상태 변경 알림 함수
     */
    private fun triggerServiceUpdate() {
        // 백그라운드 리스너 서비스 인텐트 생성
        val intent = Intent(context, BackgroundListenerService::class.java)

        try {
            // 상태 변경 알림을 위한 서비스 실행
            context.startService(intent)
        } catch (e: Exception) {
            // 서비스 가동 실패 시 상태 갱신 대기
        }
    }

    /**
     * 앱의 포어그라운드 전환 콜백 함수
     */
    override fun onStart(owner: LifecycleOwner) {
        // 활성화 상태 플래그 변경
        isAppInForeground = true

        // 서비스에 상태 변화 알림
        triggerServiceUpdate()
    }

    /**
     * 앱의 백그라운드 전환 콜백 함수
     */
    override fun onStop(owner: LifecycleOwner) {
        // 활성화 상태 플래그 변경
        isAppInForeground = false

        // 서비스에 상태 변화 알림
        triggerServiceUpdate()
    }
}