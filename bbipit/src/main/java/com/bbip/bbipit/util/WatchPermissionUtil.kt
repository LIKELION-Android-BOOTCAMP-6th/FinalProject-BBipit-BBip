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
     * 워치 권한 거부 시 시나리오 처리
     * 
     * @param context Context 객체
     * @param permission 거부된 권한
     * @param onShowToast 토스트 메시지 출력 람다
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
            onShowToast("워치 설정에서 권한을 직접 허용해주세요.")
            context.openWatchSettings()
        } else {
            onShowToast("무전 기능을 사용하려면 권한이 필요합니다.")
        }
    }

    /**
     * Wear OS 시스템의 애플리케이션 상세 설정 화면 이동
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
