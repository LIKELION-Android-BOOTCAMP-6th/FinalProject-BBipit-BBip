package com.bbip.bbipit.core.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * 기기 마이크 활용 음성 녹음 및 파일 저장 클래스
 */
class AudioRecorder(private val context: Context) {

    // 음성 녹음 담당 시스템 객체
    private var recorder: MediaRecorder? = null

    // 녹음 파일 저장 경로 관리 변수
    private val recordingFile: File by lazy {
        File(context.cacheDir, "walkie_talkie.m4a")
    }

    /**
     * 녹음 시작 함수
     */
    fun start() {
        try {
            // 기존 임시 파일 삭제
            if (recordingFile.exists()) {
                recordingFile.delete()
            }

            // 녹음기 생성 및 설정
            createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(16000)
                setOutputFile(recordingFile.absolutePath)
                prepare()
                start()

                recorder = this
                Log.d("AudioRecorder", "HE-AAC (.m4a) 녹음 시작 완료")
            }
        } catch (e: Exception) {
            // 녹음 시작 실패 예외 처리
            Log.e("AudioRecorder", "MediaRecorder 시작 실패", e)
        }
    }

    /**
     * 녹음 중단 및 파일 경로 반환 함수
     */
    fun stop(): android.net.Uri? {
        var isStopSuccessful = true

        try {
            // 녹음 중지 실행
            recorder?.let {
                it.stop()
            }
            Log.d("AudioRecorder", "MediaRecorder 정상 중지 완료")
        } catch (e: RuntimeException) {
            // 녹음 시간이 너무 짧은 경우 예외 처리 및 임시 파일 삭제
            Log.e("AudioRecorder", "녹음 시간이 너무 짧아 stop failed 처리됨 (안전하게 캐치 완료)")
            isStopSuccessful = false

            if (recordingFile.exists()) {
                recordingFile.delete()
            }
        } catch (e: Exception) {
            // 중지 중 알 수 없는 오류 예외 처리
            Log.e("AudioRecorder", "MediaRecorder 중지 중 알 수 없는 오류 발생", e)
            isStopSuccessful = false
        } finally {
            try {
                // 녹음기 자원 해제
                recorder?.release()
            } catch (ex: Exception) {
                Log.e("AudioRecorder", "MediaRecorder 자원 해제 중 에러", ex)
            }
            recorder = null
        }

        // 정상 종료 여부에 따른 파일 경로 반환
        return if (isStopSuccessful && recordingFile.exists()) {
            android.net.Uri.fromFile(recordingFile)
        } else {
            null
        }
    }

    /**
     * 안드로이드 버전별 녹음기 객체 생성 함수
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