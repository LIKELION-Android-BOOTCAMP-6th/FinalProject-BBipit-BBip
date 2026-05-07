package com.bbip.bbipit.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bbip.bbipit.R

val regular = FontFamily(
    Font(R.font.tmoney_round_wind_regular)
)
val extraBold = FontFamily(
    Font(R.font.tmoney_round_wind_extra_bold)
)
val Typography = Typography(

    //앱 이름 등 강조되는 글씨
    titleLarge = TextStyle(
        fontFamily = extraBold,
        fontSize = 40.sp,
        lineHeight = 60.sp,
        letterSpacing = -2.sp,
        color = primary
    ),

    //앱 바 타이틀등 세미 강조
    bodyLarge = TextStyle(
        fontFamily = regular,
        fontSize = 27.sp,
        color = primary,
        fontWeight = FontWeight.Bold
    ),

    //기본 글씨
    bodyMedium = TextStyle(
        fontFamily = regular,
        fontSize = 19.sp,
        lineHeight = 27.sp,
        color = fontDefault
    ),

    //세미 글씨용
    bodySmall = TextStyle(
        fontFamily = regular,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        color = fontHint
    )

)