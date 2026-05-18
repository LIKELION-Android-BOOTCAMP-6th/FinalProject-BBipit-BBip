package com.bbip.bbipit.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bbip.bbipit.R

val suit = FontFamily(
    Font(R.font.suit_variable)
)

val Typography = Typography(

    //앱 이름 등 강조되는 글씨
    titleLarge = TextStyle(
        fontFamily = suit,
        fontSize = 40.sp,
        lineHeight = 60.sp,
        letterSpacing = -2.sp,
        color = primary,
        fontWeight = FontWeight.ExtraBold
    ),

    //앱 바 타이틀등 세미 강조
    bodyLarge = TextStyle(
        fontFamily = suit,
        fontSize = 27.sp,
        color = primary,
        fontWeight = FontWeight.Bold
    ),

    //기본 글씨
    bodyMedium = TextStyle(
        fontFamily = suit,
        fontSize = 19.sp,
        lineHeight = 27.sp,
        color = fontDefault,
        fontWeight = FontWeight.Normal
    ),

    //세미 글씨용
    bodySmall = TextStyle(
        fontFamily = suit,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        color = Color.Gray,
        fontWeight = FontWeight.ExtraLight
    )

)