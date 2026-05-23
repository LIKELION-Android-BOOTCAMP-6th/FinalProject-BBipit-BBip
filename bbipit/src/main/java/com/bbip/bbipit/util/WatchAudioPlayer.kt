package com.bbip.bbipit.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * 오디오 스트리밍 재생 플레이어
 */
class WatchAudioPlayer private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: WatchAudioPlayer? = null

        /**
         * 싱글톤 인스턴스 반환
         */
        fun getInstance(context: Context): WatchAudioPlayer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WatchAudioPlayer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 미디어 플레이어 객체
    private var mediaPlayer: MediaPlayer? = null

    // 재생 스레드 및 핸들러 초기화
    private val handlerThread = HandlerThread("MediaPlayerThread").apply { start() }
    private val playerHandler = Handler(handlerThread.looper)

    /**
     * URL 기반 음성 파일 비동기 재생
     */
    fun playFromUrl(url: String, onCompletion: () -> Unit) {
        // 백그라운드 스레드에서 재생 작업 수행
        playerHandler.post {
            try {
                // 기존 플레이어 리소스 해제
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    // 오디오 속성 설정
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(url)
                    // 버퍼링 완료 시 재생 시작
                    setOnPreparedListener { it.start() }
                    // 재생 완료 시 콜백 호출 및 리소스 해제
                    setOnCompletionListener {
                        onCompletion()
                        it.release()
                    }
                    // 비동기 준비 실행
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error", e)
            }
        }
    }

    /**
     * 스레드 및 미디어 리소스 해제
     */
    fun release() {
        handlerThread.quitSafely()
        mediaPlayer?.release()
    }
}