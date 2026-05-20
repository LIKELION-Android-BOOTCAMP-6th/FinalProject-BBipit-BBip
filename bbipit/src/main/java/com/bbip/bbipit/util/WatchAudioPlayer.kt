package com.bbip.bbipit.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * 원격 스토리지 URL 로부터 오디오 데이터를 스트리밍하여 백그라운드 재생을 수행하는 미디어 플레이어
 */
class WatchAudioPlayer private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: WatchAudioPlayer? = null

        /**
         * 메모리 누수 방지 및 자원 단일화를 위한 스레드 안전 싱글톤 인스턴스 반환 팩토리
         */
        fun getInstance(context: Context): WatchAudioPlayer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WatchAudioPlayer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 오디오 재생을 제어하는 안드로이드 미디어 플레이어 객체
    private var mediaPlayer: MediaPlayer? = null

    // 메인 UI 스레드 병목 현상 방지 및 재생 안정성 확보를 위한 전용 핸들러 스레드
    private val handlerThread = HandlerThread("MediaPlayerThread").apply { start() }
    private val playerHandler = Handler(handlerThread.looper)

    /**
     * 네트워크 오디오 소스 주소를 기반으로 비동기 미디어 준비 및 재생 처리 실행
     */
    fun playFromUrl(url: String, onCompletion: () -> Unit) {
        // UI 프레임 드랍 차단을 위해 모든 미디어 파이프라인 작업을 전용 백그라운드 스레드에 할당
        playerHandler.post {
            try {
                // 중복 재생 방지를 위한 기존 미디어 플레이어 자원 해제 고립화
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    // 미디어 속성(오디오 유형 및 하드웨어 사용 목적) 정의
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(url)
                    // 스트리밍 버퍼링 완료 시점의 자동 재생 개시 리스너 등록
                    setOnPreparedListener { it.start() }
                    // 재생 종료 시점의 콜백 함수 호출 및 하드웨어 자원 반환 리스너 등록
                    setOnCompletionListener {
                        onCompletion()
                        it.release()
                    }
                    // 네트워크 지연 고려 및 메인 스레드 블로킹 방지를 위한 비동기 준비 실행
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error", e)
            }
        }
    }

    /**
     * 서비스 종료 또는 애플리케이션 폐기 시 구동 중인 백그라운드 스레드 및 미디어 자원 안전 해제
     */
    fun release() {
        handlerThread.quitSafely()
        mediaPlayer?.release()
    }
}