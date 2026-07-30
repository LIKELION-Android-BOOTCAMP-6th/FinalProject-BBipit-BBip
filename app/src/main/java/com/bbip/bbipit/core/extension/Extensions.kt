package com.bbip.bbipit.core.extension

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 현재 Context의 Base를 역추적하여 팝업/다이얼로그 렌더링 권한을 가진 진짜 Activity를 반환하는 확장 함수
 (ApplicationContext나 ContextWrapper 컴포넌트 내부에서 순수 Activity 레벨의 객체를 추출할 때 사용)
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun String.urlMapper():String{
    return if (this.startsWith("http://")) {
        this.replaceFirst("http://", "https://")
    } else {
        this
    }
}

//중복 스택 관리
fun <T : Any> NavController.navigateSingleTop(
    route: T,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    this.navigate(route) {
        launchSingleTop = true
        // 기존에 넘기려던 builder 설정(popUpTo 등)이 있다면 같이 적용
        builder()
    }
}