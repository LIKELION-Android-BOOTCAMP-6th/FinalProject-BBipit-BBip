package com.bbip.bbipit.core.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

/**
 * 서버나 로컬 파일의 음성 데이터 재생 클래스
 * Android MediaPlayer API 기반 스트리밍 재생, 상태 관리 및 자원 해제 기능 수행
 */
class AudioPlayer(private val context: Context) {

    // 음성 재생 담당 시스템 객체
    private var mediaPlayer: MediaPlayer? = null

    /**
     * 외부 URL 기반 스트리밍 재생 함수
     */
    fun playFromUrl(url: String, onCompletion: () -> Unit = {}) {
        try {
            Log.d("AudioPlayer", "Attempting to play audio from: $url")

            // 기존 재생 종료 및 초기화
            stopAudio()

            // 재생기 생성 및 설정
            mediaPlayer = MediaPlayer().apply {
                // 오디오 속성 설정
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                // 재생할 파일 경로 지정
                setDataSource(url)

                // 재생 준비 완료 시 재생 시작
                setOnPreparedListener {
                    Log.d("AudioPlayer", "Audio prepared, starting playback")
                    it.start()
                }

                // 재생 완료 시 자원 해제
                setOnCompletionListener {
                    Log.d("AudioPlayer", "Playback completed")
                    onCompletion()
                    it.release()
                    if (mediaPlayer == it) mediaPlayer = null
                }

                // 에러 발생 시 자원 해제
                setOnErrorListener { mp, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer Error: what=$what, extra=$extra")
                    mp.release()
                    if (mediaPlayer == mp) mediaPlayer = null
                    true
                }

                // 비동기 재생 준비 실행
                prepareAsync()
            }
        } catch (e: Exception) {
            // 재생 구성 실패 예외 처리
            Log.e("AudioPlayer", "Error playing audio", e)
        }
    }

    /**
     * 현재 오디오 진행 위치(밀리초) 반환 함수
     */
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    /**
     * 현재 오디오 재생 여부 반환 함수
     */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    /**
     * 오디오 재생 즉시 중단 및 미디어 자원 반납 함수
     */
    fun stopAudio() {
        // 재생 정지 및 자원 해제
        mediaPlayer?.stop()
        mediaPlayer?.release()

        // 인스턴스 초기화
        mediaPlayer = null
    }
}