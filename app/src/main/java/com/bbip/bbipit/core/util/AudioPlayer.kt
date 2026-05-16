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
    // 음성 재생 담당 프레임워크 객체
    private var mediaPlayer: MediaPlayer? = null

    /**
     * 외부 URL을 통한 스트리밍 재생 수행
     * 네트워크 상태 고려 비동기 준비(prepareAsync) 방식 사용 및 완료 시 자동 재생 시작
     * @param url 재생할 음성 파일 경로
     * @param onCompletion 재생 완료 콜백
     */
    fun playFromUrl(url: String, onCompletion: () -> Unit = {}) {
        try {
            Log.d("AudioPlayer", "Attempting to play audio from: $url")
            // 재생 전 기존 자원 정리
            stopAudio()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                // 음성 파일 경로 설정
                setDataSource(url)

                // 버퍼링 완료 시 재생 시작 정의
                setOnPreparedListener {
                    Log.d("AudioPlayer", "Audio prepared, starting playback")
                    it.start()
                }

                // 재생 완료 시 자원 해제 및 콜백 호출 정의
                setOnCompletionListener {
                    Log.d("AudioPlayer", "Playback completed")
                    onCompletion()
                    it.release()
                    if (mediaPlayer == it) mediaPlayer = null
                }

                // 오류 발생 시 자원 해제 및 에러 처리 알림
                setOnErrorListener { mp, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer Error: what=$what, extra=$extra")
                    mp.release()
                    if (mediaPlayer == mp) mediaPlayer = null
                    true
                }

                // 네트워크 데이터 로딩을 위한 비동기 준비 실행
                prepareAsync()
            }
        } catch (e: Exception) {
            // URL 오류 및 네트워크 예외 기록
            Log.e("AudioPlayer", "Error playing audio", e)
        }
    }

    /**
     * 현재 오디오 진행 위치(밀리초) 반환
     */
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    /**
     * 현재 오디오 재생 여부 반환
     */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    /**
     * 오디오 재생 즉시 중단 및 자원 반납
     *
     * 화면 전환이나 정지 버튼 호출 시 하드웨어 자원 낭비 방지 및 상태 초기화
     */
    fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}