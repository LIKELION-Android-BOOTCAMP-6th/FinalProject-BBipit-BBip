package com.bbip.bbipit.base

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * 사용자 프로필 이미지를 포함하는 구글맵 커스텀 마커 BitmapDescriptor 생성용 헬퍼 함수
 */
suspend fun createCustomMarkerBitmap(
    context: android.content.Context,
    imageUrl: String,
    isOnline: Boolean
): com.google.android.gms.maps.model.BitmapDescriptor {
    val density = context.resources.displayMetrics.density

    // 하단 지시선 삼각형 공간 확보를 고려한 마커 도화지 크기 설정
    val markerWidth = (54 * density).toInt()
    val markerHeight = (64 * density).toInt()

    val bitmap = Bitmap.createBitmap(markerWidth, markerHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 상태값(온라인/오프라인)에 따른 외곽 테두리 색상 분기 정의
    val markerColor = if (isOnline) {
        Color(0xFF956AFC).toArgb() // 온라인 상태: 보라색 테두리
    } else {
        Color(0xFF94A3B8).toArgb() // 오프라인 상태: 회색 테두리
    }

    // 마커 외곽선 및 지시선 드로잉 전용 Paint 객체
    val paint = Paint().apply {
        isAntiAlias = true
        color = markerColor
    }

    val centerX = markerWidth / 2f
    val circleRadius = markerWidth / 2f // 테두리를 포함하는 메인 원형 반지름
    val circleCenterY = circleRadius    // 상단 배치를 위한 원형 중심 Y좌표

    val triangleWidth = 24 * density  // 지시선 가로 폭

    // 원형 하단 경계선에서 맨 아래 꼭지점으로 이어지는 지시선 경로 설정
    val path = android.graphics.Path().apply {
        moveTo(centerX - (triangleWidth / 2f), circleCenterY + (circleRadius * 0.8f))
        lineTo(centerX, markerHeight.toFloat() - (2 * density)) // 최하단 중앙 뾰족한 꼭지점 좌표
        lineTo(centerX + (triangleWidth / 2f), circleCenterY + (circleRadius * 0.8f))
        close()
    }
    canvas.drawPath(path, paint)

    // 마커의 상단 메인 외곽 테두리 원 드로잉
    canvas.drawCircle(centerX, circleCenterY, circleRadius, paint)

    // 테두리 두께를 제외한 내부 프로필 원 영역 치수 계산
    val borderThickness = 3.5f * density
    val innerRadius = circleRadius - borderThickness
    val profileSize = (innerRadius * 2).toInt()

    // 프로필 이미지 부재 시 대체할 Firebase 기본 원형 아이콘 경로 지정
    val targetUrl = imageUrl.ifEmpty {
        "https://firebasestorage.googleapis.com/v0/b/bbipit.firebasestorage.app/o/profile_images%2Fic_person.PNG?alt=media&token=065d1189-6f34-42a9-b0fa-a6da41b816bf"
    }

    // Canvas 조작 및 드로잉을 위한 하드웨어 비트맵 방지 처리 포함 이미지 요청 빌드
    val request = ImageRequest.Builder(context)
        .data(targetUrl)
        .allowHardware(false) // 렌더링 에러 방지를 위한 소프트웨어 비트맵 고정
        .build()

    // 코일 이미지 로더 인스턴스를 통한 원격/캐시 리소스 동기 획득 처리
    val drawable = context.imageLoader.execute(request).drawable

    // 이미지 로드 결과에 따른 비트맵 추출 및 로드 실패 대응 예외 처리
    val profileBitmap = if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else {
        // 비정상 이미지 혹은 네트워크 에러 발생 시 앱 크래시 방지용 투명 플레이스홀더 생성
        Bitmap.createBitmap(profileSize, profileSize, Bitmap.Config.ARGB_8888)
    }

    // 마스크 내경 크기에 맞춘 원본 이미지 크기 조정
    val scaledProfileBitmap = Bitmap.createScaledBitmap(profileBitmap, profileSize, profileSize, false)

    // 원형 크롭을 위한 마스킹 전용 투명 보드 생성
    val maskBitmap = Bitmap.createBitmap(profileSize, profileSize, Bitmap.Config.ARGB_8888)
    val maskCanvas = Canvas(maskBitmap)
    val maskPaint = Paint().apply { isAntiAlias = true }

    // 마스킹 기준점이 될 내경 크기의 원형 드로잉
    maskCanvas.drawCircle(innerRadius, innerRadius, innerRadius, maskPaint)

    // 지정 원형 영역 내부에만 비트맵이 표현되도록 하는 포터더프 엑스퍼모드 모드 결합
    maskPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
    maskCanvas.drawBitmap(scaledProfileBitmap, 0f, 0f, maskPaint)

    // 원형으로 컷팅된 최종 프로필 이미지를 메인 마커 테두리 중앙부에 합성
    canvas.drawBitmap(maskBitmap, centerX - innerRadius, circleCenterY - innerRadius, null)

    // 구글맵 마커 전달용 BitmapDescriptor 객체 변환 및 반환
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}