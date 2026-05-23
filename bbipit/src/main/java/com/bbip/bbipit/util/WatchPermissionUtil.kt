package com.bbip.bbipit.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat

/**
 * 워치 앱 권한 처리 유틸리티
 */
object WatchPermissionUtil {

    /**
     * 권한 거부 시나리오 처리
     */
    fun handlePermissionDenial(
        context: Context,
        permission: String,
        onShowToast: (String) -> Unit
    ) {
        val activity = context as? Activity
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
        } ?: true

        if (!shouldShowRationale) {
            // 영구 거부 상태일 경우 설정 화면 이동
            onShowToast("워치 설정에서 권한을 직접 허용해주세요.")
            context.openWatchSettings()
        } else {
            // 일반 거부 상태일 경우 안내 문구 표시
            onShowToast("무전 기능을 사용하려면 권한이 필요합니다.")
        }
    }

    /**
     * 워치 애플리케이션 상세 설정 화면 이동
     */
    fun Context.openWatchSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }
}