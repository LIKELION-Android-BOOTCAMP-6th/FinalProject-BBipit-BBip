package com.bbip.bbipit.core.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * 기기 마이크를 활용한 음성 녹음 및 파일 저장 클래스
 */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    /**
     * 녹음 시작 및 지정 파일 저장
     * .m4a 확장자 호환을 위한 MPEG_4/AAC 규격 강제 지정 및
     * 고효율 HE_AAC 인코딩과 16kHz 샘플링 레이트 적용
     * @param outputFile 저장될 오디오 파일 객체
     */
    fun start(outputFile: File) {
        try {
            createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)

                // .m4a 포맷 및 디코더 동기화를 위한 출력 포맷 설정
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                // 저비트레이트 고효율을 위한 HE_AAC 인코더 적용
                setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)

                // 무전기 품질 최적화를 위한 16kHz 샘플링 레이트 설정
                setAudioSamplingRate(16000)

                // 파일 크기 최적화 및 업로드 속도 향상을 위한 비트레이트 하향 조정
                setAudioEncodingBitRate(16000)

                setOutputFile(outputFile.absolutePath)
                prepare()
                start()

                recorder = this
                Log.d("AudioRecorder", "HE-AAC (.m4a) 녹음 시작 완료")
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "MediaRecorder 시작 실패", e)
        }
    }

    /**
     * 녹음 중단 및 자원 해제
     */
    fun stop() {
        try {
            recorder?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "MediaRecorder 중지 중 오류 발생 (녹음 시간 부족 등)", e)
        } finally {
            recorder = null
        }
    }

    /**
     * 안드로이드 OS 버전에 적합한 MediaRecorder 생성
     */
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}