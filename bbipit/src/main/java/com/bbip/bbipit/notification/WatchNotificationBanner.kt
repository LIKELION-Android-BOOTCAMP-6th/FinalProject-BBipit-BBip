package com.bbip.bbipit.notification

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.primary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WatchNotificationBanner(
    item: Notification,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    // 자동 시간 만료 기능
    LaunchedEffect(key1 = item.id) {
        delay(4000L)
        onDismiss()
    }

    // 밑에서 위로 등장 및 퇴장 애니메이션 기능
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 800)
        ) + fadeIn(animationSpec = tween(durationMillis = 500)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 600)
        ) + fadeOut(animationSpec = tween(durationMillis = 300)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 5.dp, end = 5.dp),
            contentAlignment = Alignment.Center // 새로운 기능: 배너가 화면 중앙에 안착하도록 설정
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .heightIn(max = 120.dp) // 새로운 기능: 글이 길어져도 테두리가 화면 밖으로 짤리지 않게 최대 높이 제한
                    .clip(RoundedCornerShape(28.dp))
                    .background(background)
                    .clickable { onClick() }
                    .padding(top = 12.dp, bottom = 14.dp, start = 30.dp, end = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 1. 상단 프로필
                Box(
                    modifier = Modifier
                        .size(23.dp)
                        .clip(CircleShape)
                        .background(primary),
                    contentAlignment = Alignment.Center
                ) {}

                Spacer(modifier = Modifier.height(6.dp))

                // 2. 발신자 이름
                Text(
                    text = item.senderName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 3. 시간 정보
                val timeString = remember(item.createdAt) {
                    SimpleDateFormat("aa h:mm", Locale.KOREAN).format(Date(item.createdAt))
                }
                Text(
                    text = timeString,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 4. 알림 유형별 본문 분기
                val bodyText = when (item.type) {
                    "REQ" -> "친구 요청이 왔어요."
                    else -> item.content
                }

                Text(
                    text = bodyText,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(
    device = androidx.wear.tooling.preview.devices.WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xFF000000,
    showBackground = true
)
@Composable
fun WatchNotificationBannerPreview() {
    val mockNotification = Notification(
        id = "1",
        senderName = "Sender",
        type = "DM",
        content = "비도 오고 그래서 니 생각이 나서 그래서 그랬던 거지 별 의미 없지",
        createdAt = System.currentTimeMillis(),
        isRead = false,
        roomId = null
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WatchNotificationBanner(
            item = mockNotification,
            modifier = Modifier.fillMaxSize(),
            onDismiss = {},
            onClick = {}
        )
    }
}