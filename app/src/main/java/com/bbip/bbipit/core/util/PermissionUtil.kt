package com.bbip.bbipit.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ActivityCompat
import android.provider.Settings

/**
 * 앱 권한 요청 및 처리 지원을 위한 유틸리티 객체
 */
object PermissionUtil {
    /**
     * 권한 거부 상황 분석 및 처리
     * 권한 요청 거부 사유 분석을 통해 사용자에게 메시지 제공 또는 
     * 앱 설정 화면으로 유도하여 권한 직접 허용 처리
     * @param activity 현재 수행 중인 Activity
     * @param permission 요청된 권한 식별자
     * @param onShowToast 사용자 피드백을 위한 토스트 메시지 출력 콜백
     */
    fun handlePermissionDenial(
        activity: Activity,
        permission: String,
        onShowToast: (String) -> Unit
    ) {
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

        if (!shouldShowRationale) {
            // 더 이상 요청 불가능한 상태 시 설정 화면으로 유도
            onShowToast("설정에서 권한을 직접 허용해주세요.")
            activity.openAppSettings()
        } else {
            // 권한 필요 사유 안내
            onShowToast("기능 사용을 위해 권한이 필요합니다.")
        }
    }

    /**
     * 애플리케이션 상세 설정 화면으로 이동
     * 호출 시 사용자를 해당 앱의 시스템 권한 설정 페이지로 전환
     */
    fun Context.openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}