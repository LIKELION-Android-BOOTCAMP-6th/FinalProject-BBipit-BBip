package com.bbip.bbipit.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

/**
 * 서버나 로컬 파일로부터 음성 데이터를 가져와 재생하는 기능 담당
 */
class WatchAudioPlayer(private val context: Context) {
    /** 음성 재생 수행 안드로이드 프레임워크 객체 */
    private var mediaPlayer: MediaPlayer? = null

    /**
     * 외부 네트워크 주소를 통한 음성 파일 스트리밍 재생
     * 네트워크 상태 고려 비동기 준비 방식 사용 및 준비 완료 시 자동 재생 시작
     */
    fun playFromUrl(url: String, onCompletion: () -> Unit = {}) {
        try {
            Log.d("AudioPlayer", "Attempting to play audio from: $url")
            /** 기존 재생 자원 정리 */
            stopAudio()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                /** 음성 파일 웹 경로 설정 */
                setDataSource(url)

                /** 오디오 데이터 버퍼링 및 준비 완료 시 동작 정의 */
                setOnPreparedListener {
                    Log.d("AudioPlayer", "Audio prepared, starting playback")
                    /** 준비 완료 후 소리 재생 시작 */
                    it.start()
                }

                /** 재생 완료 시 동작 정의 */
                setOnCompletionListener {
                    Log.d("AudioPlayer", "Playback completed")
                    /** 재생 종료 후 자원 해제 */
                    onCompletion()
                    it.release()
                    if (mediaPlayer == it) mediaPlayer = null
                }

                setOnErrorListener { mp, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer Error: what=$what, extra=$extra")
                    mp.release()
                    if (mediaPlayer == mp) mediaPlayer = null
                    true
                }

                /** 메인 스레드 차단 방지를 위한 비동기 준비 */
                prepareAsync()
            }
        } catch (e: Exception) {
            /** 오류 발생 시 예외 정보 기록 */
            Log.e("AudioPlayer", "Error playing audio", e)
        }
    }

    /**
     * 현재 재생 중인 오디오 진행 위치 반환
     */
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    /**
     * 현재 오디오 재생 여부 반환
     */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    /**
     * 오디오 재생 중단 및 자원 반납
     * 앱 화면 종료 시 호출하여 하드웨어 자원 효율적 관리 및 상태 초기화
     */
    fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
