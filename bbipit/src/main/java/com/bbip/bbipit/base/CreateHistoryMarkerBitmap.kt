package com.bbip.bbipit.base

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.bbip.bbipit.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun createHistoryMarkerBitmap(context: Context, category: String): BitmapDescriptor {
    val (iconResId, bgColor, iconColor) = when (category) {
        "무전" -> Triple(R.drawable.ic_walkie_talkie_icon, 0xFFFAF5FF.toInt(), 0xFFA855F7.toInt())
        "카페" -> Triple(R.drawable.ic_cafe_icon, 0xFFFFFBEB.toInt(), 0xFFD97706.toInt())
        "음식" -> Triple(R.drawable.ic_restaurant_icon, 0xFFFFF1F2.toInt(), 0xFFF43F5E.toInt())
        "운동" -> Triple(R.drawable.ic_exercise_icon, 0xFFECFDF5.toInt(), 0xFF10B981.toInt())
        else -> Triple(R.drawable.ic_daily_icon, 0xFFEEF2FF.toInt(), 0xFF6366F1.toInt())
    }

    // 💡 1. 시인성 확보를 위해 전체 마커 픽셀 사이즈 증가 (기존 64 -> 80)
    // 그림자와 외곽선 영역 공간을 확보하기 위함입니다.
    val baseSize = 80
    val bitmap = Bitmap.createBitmap(baseSize, baseSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = baseSize / 2f

    // 💡 2. 그림자(Shadow) 레이어 Paint 설정
    val shadowPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.FILL
        // 하드웨어 가속 상태에서 그림자가 정상 표현되도록 blur 및 offset 설정
        setShadowLayer(6f, 0f, 4f, Color.argb(80, 0, 0, 0))
    }
    // 본체 영역보다 아주 약간 작게 그림자 베이스 원을 먼저 그려줍니다.
    val contentRadius = baseSize * 0.42f
    canvas.drawCircle(center, center, contentRadius, shadowPaint)

    // 💡 3. 흰색 외곽선(Border) Paint 설정
    val borderPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL // 원 전체를 채운 후 위에 카테고리 컬러를 덮는 방식
    }
    canvas.drawCircle(center, center, contentRadius, borderPaint)

    // 💡 4. 알맹이(카테고리 배경 컬러 원) Paint 설정
    val innerPaint = Paint().apply {
        color = bgColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    // 외곽선 두께만큼 반전(Padding)을 주기 위해 반지름을 3~4픽셀 줄여서 그립니다.
    val innerRadius = contentRadius - 4f
    canvas.drawCircle(center, center, innerRadius, innerPaint)

    // 💡 5. 내부 아이콘 드로우 및 센터링 정렬
    val drawable = ContextCompat.getDrawable(context, iconResId)
    drawable?.let {
        it.setTint(iconColor)
        // 안쪽 원 크기에 맞춰 아이콘 마진 동적 조율
        val iconSize = (innerRadius * 1.1f).toInt()
        val left = (center - iconSize / 2f).toInt()
        val top = (center - iconSize / 2f).toInt()

        it.setBounds(left, top, left + iconSize, top + iconSize)
        it.draw(canvas)
    }

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}