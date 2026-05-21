package com.bbip.bbipit.core.base

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
import com.bbip.bbipit.core.ui.theme.primary
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * 구글 맵 컴포넌트 노출용 원형 프로필 및 상태 테두리 혼합 개인화 마커 비동기 생성 헬퍼 함수
 */
suspend fun createCustomMarkerBitmap(
    context: android.content.Context,
    imageUrl: String,
    isOnline: Boolean
): com.google.android.gms.maps.model.BitmapDescriptor {
    // 해상도 격차에 따른 그래픽 축소 현상 방지용 디바이스 화면 밀도 규격 조회
    val density = context.resources.displayMetrics.density

    // 하단 말풍선 꼬리 핀 지시선 영역 확보를 위한 도화지 크기 설정 (세로 길이 상향 조정)
    val markerWidth = (54 * density).toInt()
    val markerHeight = (64 * density).toInt()

    val bitmap = Bitmap.createBitmap(markerWidth, markerHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 사용자 온라인 접속 여부 기준 테두리 장식용 테마 컬러 에셋 동적 변환
    val markerColor = if (isOnline) {
        primary.toArgb()
    } else {
        Color(0xFF94A3B8).toArgb()
    }

    // 원형 곡선 계단 현상 제거용 안티에일리어싱 필터 활성화 및 브러시 색상 충전
    val paint = Paint().apply {
        isAntiAlias = true
        color = markerColor
    }

    val centerX = markerWidth / 2f
    val circleRadius = markerWidth / 2f
    val circleCenterY = circleRadius

    val triangleHeight = 10 * density
    val triangleWidth = 24 * density

    // 원형 테두리 하단부에서 마커 최하단 정중앙으로 떨어지는 뾰족한 삼각형 바늘 핀 제도
    val path = android.graphics.Path().apply {
        moveTo(centerX - (triangleWidth / 2f), circleCenterY + (circleRadius * 0.8f))
        lineTo(centerX, markerHeight.toFloat() - (2 * density))
        lineTo(centerX + (triangleWidth / 2f), circleCenterY + (circleRadius * 0.8f))
        close()
    }
    canvas.drawPath(path, paint)

    // 상단 캔버스 영역 내 메인 프로필 배경 원형 프레임 레이어링
    canvas.drawCircle(centerX, circleCenterY, circleRadius, paint)

    // 프로필 이미지 삽입용 안쪽 원 크기 산출 (내경 반지름 축소 계산)
    val borderThickness = 3.5f * density
    val innerRadius = circleRadius - borderThickness
    val profileSize = (innerRadius * 2).toInt()

    // 네트워크 경로 주소 부재 시 활용할 기본 제공 대체 프로필 이미지 주소 바인딩
    val targetUrl = imageUrl.ifEmpty {
        "https://firebasestorage.googleapis.com/v0/b/bbipit.firebasestorage.app/o/profile_images%2Fic_person.PNG?alt=media&token=065d1189-6f34-42a9-b0fa-a6da41b816bf"
    }

    val request = ImageRequest.Builder(context)
        .data(targetUrl)
        .allowHardware(false)
        .build()

    // 이미지 로딩 라이브러리(Coil) 동기 실행기 구동을 통한 웹 저장소 또는 디스크 캐시 리소스 인출
    val drawable = context.imageLoader.execute(request).drawable

    // 예기치 않은 오류 발생 시 런타임 크래시 방지용 투명 공백 보드 배치 및 비트맵 형변환
    val profileBitmap = if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else {
        Bitmap.createBitmap(profileSize, profileSize, Bitmap.Config.ARGB_8888)
    }

    // 마커 내경 프레임 치수 부합을 위한 비트맵 가로 세로 스케일 균등 재조정
    val scaledProfileBitmap = Bitmap.createScaledBitmap(profileBitmap, profileSize, profileSize, false)

    // 원형 크롭 마스킹 연산 전담용 투명 알파 채널 가상 비트맵 보드 가설
    val maskBitmap = Bitmap.createBitmap(profileSize, profileSize, Bitmap.Config.ARGB_8888)
    val maskCanvas = Canvas(maskBitmap)
    val maskPaint = Paint().apply { isAntiAlias = true }

    // 마스킹용 캔버스 위 속이 꽉 찬 형태의 기본 내경용 마스터 원 작도
    maskCanvas.drawCircle(innerRadius, innerRadius, innerRadius, maskPaint)

    // 지정 원 범위 내 유효 픽셀 결합 마스킹 목적의 포터더프 소스-인(SRC_IN) 혼합 모드 설정
    maskPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
    maskCanvas.drawBitmap(scaledProfileBitmap, 0f, 0f, maskPaint)

    // 원형 테두리 정중앙 지점 내 최종 가공 완료 사용자 사진 그래픽 레이어 투하
    canvas.drawBitmap(maskBitmap, centerX - innerRadius, circleCenterY - innerRadius, null)

    // 구글 지도 런타임 컴포넌트 판독 가능 규격인 비트맵 디스크립터 구조 변환 및 최종 리턴
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}