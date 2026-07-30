package com.bbip.bbipit.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bbip.bbipit.R

val pretendard = FontFamily(
    Font(R.font.pretendard_thin, FontWeight.Thin),
    Font(R.font.pretendard_light, FontWeight.Light),
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_bold, FontWeight.ExtraBold),
    Font(R.font.pretendard_black, FontWeight.Black),

)

val Typography = Typography(

    //앱 이름 등 강조되는 글씨
    titleLarge = TextStyle(
        fontFamily = pretendard,
        fontSize = 40.sp,
        lineHeight = 60.sp,
        letterSpacing = -2.sp,
        color = primary,
        fontWeight = FontWeight.ExtraBold
    ),
    //캐치프라이즈
    titleSmall = TextStyle(
      fontFamily = pretendard,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        color = Color.DarkGray,
        fontWeight = FontWeight.Thin
    ),

    //앱 바 타이틀등 세미 강조
    bodyLarge = TextStyle(
        fontFamily = pretendard,
        fontSize = 32.sp,
        color = primary,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.Bold
    ),

    //기본 글씨
    bodyMedium = TextStyle(
        fontFamily = pretendard,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        color = fontDefault,
        fontWeight = FontWeight.Normal
    ),

    //세미 글씨, 힌트용
    bodySmall = TextStyle(
        fontFamily = pretendard,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        color = Color.DarkGray,
        fontWeight = FontWeight.Thin
    ),

    //시간 등 작은 글씨
    labelSmall = TextStyle(
        fontFamily = pretendard,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = Color.DarkGray,
        fontWeight = FontWeight.Thin
    )

)