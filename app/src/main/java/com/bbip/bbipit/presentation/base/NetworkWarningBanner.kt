package com.bbip.bbipit.presentation.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 *  네트워크 연결 끊김 상태를 처리하는 배너
 */
@Composable
fun BoxScope.NetworkWarningBanner(
    showBottomBar: Boolean,
    innerPadding: PaddingValues,
    viewModel: NetworkWarningBannerViewModel = hiltViewModel() // 👈 Hilt를 통해 자동으로 뷰모델 주입
) {
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = !isNetworkConnected,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(
                bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 16.dp,
                start = 16.dp,
                end = 16.dp
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(
                    color = Color(0xE6D32F2F),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "네트워크 연결이 끊겼습니다. 상태를 확인해 주세요.",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}