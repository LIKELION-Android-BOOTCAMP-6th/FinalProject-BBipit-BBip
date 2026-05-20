package com.bbip.bbipit.core.base

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 실시간 접속 상태 및 하트비트(Heartbeat) 신호 전송 전역 관리 클래스
 * 일정 주기별 서버 활성 상태 동기화 및 현재 활성화된 채팅방 정보 추적 목적
 */
@Singleton
class LifeCycleManager @Inject constructor(
    private val liveStatusRepository: LiveStatusRepository
) {
    private val TAG = "LifeCycleManager"
    private val auth = FirebaseAuth.getInstance()

    // UI 스레드 내 주기적 작업 예약을 위한 메인 루퍼(Main Looper) 연동 핸들러 객체
    private val handler = Handler(Looper.getMainLooper())

    // 반복적 하트비트 전송 작업 수행용 실행 가능(Runnable) 객체
    private var heartbeatRunnable: Runnable? = null

    // 네트워크 통신 및 데이터베이스 작업 비동기 처리용 백그라운드 코루틴 스코프
    private val sessionScope = CoroutineScope(Dispatchers.IO)

    // 하트비트 신호 전송 주기 간격 설정 값 (30초)
    private val HEARTBEAT_INTERVAL = 30000L

    // 사용자 현재 진입 채팅방 고유 식별자(ID) 관리 변수
    private var currentRoomId: String? = null

    /**
     * 사용자 진입·이탈 채팅방 ID 정보 실시간 갱신 함수
     * 방 정보 변경 즉시 백엔드 서버 대상 현재 상태 반영 하트비트 신호 1회 송신 목적
     */
    fun updateCurrentRoom(roomId: String?) {
        this.currentRoomId = roomId
        triggerHeartbeat()
    }

    /**
     * 앱 포어그라운드 진입 시점 호출 실시간 라이브 세션 가동 함수
     * 인증 사용자 존재 시 기존 루프 제거 및 30초 주기 신규 하트비트 반복 루프 개시 목적
     */
    fun startSession() {
        if (auth.currentUser == null) return
        Log.d(TAG, "🟢 전역 라이브 세션 가동 (하트비트 루프 시작)")

        stopHeartbeatLoop()

        // 주기적 하트비트 함수 호출 및 자기 자신 재예약 구조의 재귀 형태 Runnable 정의
        heartbeatRunnable = object : Runnable {
            override fun run() {
                triggerHeartbeat()
                handler.postDelayed(this, HEARTBEAT_INTERVAL)
            }
        }
        handler.post(heartbeatRunnable!!)
    }

    /**
     * 앱 백그라운드 이탈 시점 호출 가동 중 라이브 세션 중단 함수
     * 대기 중인 모든 하트비트 예약 작업 취소를 통한 불필요 네트워크 리소스 소모 방지 목적
     */
    fun stopSession() {
        Log.d(TAG, "🔴 전역 라이브 세션 중단 (하트비트 중단)")
        stopHeartbeatLoop()
    }

    /**
     * 인증 사용자 상태 확인 후 서버 대상 실시간 수명 주기 정보 전송 함수
     * 백엔드 내 현재 체류 채팅방 ID 동기화를 위한 IO 스레드 기반 비동기 리포지토리 호출 목적
     */
    private fun triggerHeartbeat() {
        if (auth.currentUser == null) return
        sessionScope.launch {
            liveStatusRepository.updateLifeCycle(currentRoomId)
        }
    }

    /**
     * 메인 스레드 핸들러 내 대기 중 하트비트 Runnable 콜백 안전 제거 및 메모리 해제 함수
     */
    private fun stopHeartbeatLoop() {
        heartbeatRunnable?.let {
            handler.removeCallbacks(it)
            heartbeatRunnable = null
        }
    }
}