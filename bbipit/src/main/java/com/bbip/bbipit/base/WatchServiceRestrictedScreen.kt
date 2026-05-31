package com.bbip.bbipit.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState

@Composable
fun WatchServiceRestrictedScreen(
    onExitClick: () -> Unit
) {
    // Wear OS는 화면이 둥글기 때문에 스크롤이 가능한 ScalingLazyColumn을 사용하는 것이 표준입니다.
    val listState = rememberScalingLazyListState()

    // 테마 컬러 매핑 (모바일 디자인 유지)
    val slate400 = Color(0xFF94A3B8)
    val slate500 = Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // 워치는 배터리 절약을 위해 기본 배경을 검은색으로 권장합니다.
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // 1. 경고 아이콘
            item {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.WarningAmber,
                    contentDescription = "Warning",
                    tint = slate400,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(bottom = 4.dp)
                )
            }

            // 2. 제목
            item {
                Text(
                    text = "연동 서비스 불가",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // 3. 설명문 (워치 화면 특성상 문구를 간결하게 축소)
            item {
                Text(
                    text = "휴대폰의 앱이\n꺼져있거나 권한이 부족하여\n서비스를 이용할 수 없습니다.\n휴대폰 상태를 확인해주세요.",
                    fontSize = 12.sp,
                    color = slate500,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // 4. 여백 생성
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 5. 앱 종료 버튼 (워치 전용 콤팩트 버튼 디자인)
            item {
                FilledTonalButton(
                    onClick = onExitClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF202024),
                        contentColor = slate400
                    )
                ) {
                    Text(
                        text = "워치 앱 종료",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}