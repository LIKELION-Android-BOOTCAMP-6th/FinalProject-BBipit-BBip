package com.bbip.bbipit.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bbip.bbipit.R
import com.bbip.bbipit.core.ui.theme.fontDefault
import com.bbip.bbipit.core.ui.theme.primary
import androidx.wear.compose.material.Typography

val suit = FontFamily(
    Font(R.font.suit_thin, FontWeight.Thin),
    Font(R.font.suit_light, FontWeight.Light),
    Font(R.font.suit_regular, FontWeight.Normal),
    Font(R.font.suit_medium, FontWeight.Medium),
    Font(R.font.suit_semibold, FontWeight.SemiBold),
    Font(R.font.suit_bold, FontWeight.Bold),
    Font(R.font.suit_extrabold, FontWeight.ExtraBold),
)

val Typography = Typography(
    title1 = TextStyle(
        fontFamily = suit,
        fontSize = 40.sp,
        lineHeight = 60.sp,
        letterSpacing = -2.sp,
        color = primary,
        fontWeight = FontWeight.ExtraBold
    ),
    title2 = TextStyle(
        fontFamily = suit,
        fontSize = 27.sp,
        color = primary,
        fontWeight = FontWeight.Bold
    ),
    body1 = TextStyle(
        fontFamily = suit,
        fontSize = 19.sp,
        lineHeight = 27.sp,
        color = fontDefault,
        fontWeight = FontWeight.Normal
    ),
    body2 = TextStyle(
        fontFamily = suit,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        color = Color.Gray,
        fontWeight = FontWeight.ExtraLight
    )
)