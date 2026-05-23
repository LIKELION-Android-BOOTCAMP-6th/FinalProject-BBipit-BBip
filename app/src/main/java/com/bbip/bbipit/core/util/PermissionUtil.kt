package com.bbip.bbipit.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ActivityCompat
import android.provider.Settings

/**
 * 앱 권한 요청 및 처리 지원 유틸리티 객체
 */
object PermissionUtil {

    /**
     * 권한 거부 상황 분석 및 처리 함수
     */
    fun handlePermissionDenial(
        activity: Activity,
        permission: String,
        onShowToast: (String) -> Unit
    ) {
        // 권한 재요청 가능 여부 확인
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

        if (!shouldShowRationale) {
            // 거부되어 재요청이 불가능한 경우 설정 화면으로 유도
            onShowToast("설정에서 권한을 직접 허용해주세요.")
            activity.openAppSettings()
        } else {
            // 단순 거부인 경우 권한 필요 사유 안내
            onShowToast("기능 사용을 위해 권한이 필요합니다.")
        }
    }

    /**
     * 애플리케이션 상세 설정 화면 이동 함수
     */
    fun Context.openAppSettings() {
        // 설정 화면 이동을 위한 데이터 구성
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 설정 화면 실행
        startActivity(intent)
    }
}