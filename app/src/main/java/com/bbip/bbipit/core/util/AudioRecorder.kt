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
    private var recorder: MediaRecorder? = null

    // 고정 싱글 파일 형태의 내부 파일 경로 관리 변수
    private val recordingFile: File by lazy {
        File(context.cacheDir, "walkie_talkie.m4a")
    }

    /**
     * 녹음 시작 함수
     * 중복 녹음 방지 목적의 기존 임시 파일 존재 여부 확인 및 안전 삭제 처리 포함
     */
    fun start() {
        try {
            // 새로 녹음할 때 기존 임시 파일이 있다면 안전하게 삭제
            if (recordingFile.exists()) {
                recordingFile.delete()
            }

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
            Log.e("AudioRecorder", "MediaRecorder 시작 실패", e)
        }
    }

    /**
     * 녹음 중단 및 녹음 완료 파일의 안드로이드 Uri 반환 함수
     * 단시간 녹음 오류 등 실패 시 null 반환 목적
     */
    fun stop(): android.net.Uri? {
        var isStopSuccessful = true

        try {
            recorder?.let {
                // 너무 짧게 누르면 Native Method 예외 발생 가능성 존재
                it.stop()
            }
            Log.d("AudioRecorder", "MediaRecorder 정상 중지 완료")
        } catch (e: RuntimeException) {
            // 오디오 프레임 부재 시 stop 실패 예외를 던지는 안드로이드 프레임워크 내장 규격 대응
            Log.e("AudioRecorder", "녹음 시간이 너무 짧아 stop failed 처리됨 (안전하게 캐치 완료)")
            isStopSuccessful = false

            // 데이터 무효화 처리를 위한 불완전 잔여 파일 제거
            if (recordingFile.exists()) {
                recordingFile.delete()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "MediaRecorder 중지 중 알 수 없는 오류 발생", e)
            isStopSuccessful = false
        } finally {
            try {
                // 차기 녹음 정상 동작 보장 목적의 하드웨어 자원 의무 해제 처리
                recorder?.release()
            } catch (ex: Exception) {
                Log.e("AudioRecorder", "MediaRecorder 자원 해제 중 에러", ex)
            }
            recorder = null
        }

        // 정상 중지 완료 및 파일 정산 존재 여부 검증 기반의 Uri 조건부 제공
        return if (isStopSuccessful && recordingFile.exists()) {
            android.net.Uri.fromFile(recordingFile)
        } else {
            null
        }
    }

    /**
     * 안드로이드 운영체제(OS) 버전별 적합 구조의 MediaRecorder 생성 함수
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