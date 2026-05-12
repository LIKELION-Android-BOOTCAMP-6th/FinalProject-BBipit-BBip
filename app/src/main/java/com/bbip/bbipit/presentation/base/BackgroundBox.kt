package com.bbip.bbipit.presentation.base

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bbip.bbipit.core.ui.theme.backLeft
import com.bbip.bbipit.core.ui.theme.backRight

@Composable
fun BackgroundBox(
    modifier: Modifier = Modifier,
    colorA: Color = backLeft, // 좌상 & 우하
    colorB: Color = backRight, // 우상 & 좌하
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            //좌상 -> 우하
            val brush1 = Brush.linearGradient(
                0.0f to colorA,
                0.5f to colorB,
                1.0f to colorA,
                start = Offset(-200f, -200f),
                end = Offset(size.width + 200f, size.height + 200f)
            )

            //우상 -> 좌하
            val brush2 = Brush.linearGradient(
                0.0f to colorB,
                0.4f to Color.Transparent, //중앙 부분. 색
                0.6f to Color.Transparent,
                1.0f to colorB,
                start = Offset(size.width, 0f),
                end = Offset(0f, size.height)
            )

            drawRect(brush = brush1)
            drawRect(brush = brush2)
        }

        // 실제 UI 요소들이 올라갈 공간
        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}