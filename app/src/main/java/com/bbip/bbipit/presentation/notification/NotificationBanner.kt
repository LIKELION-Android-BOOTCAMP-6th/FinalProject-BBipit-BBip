package com.bbip.bbipit.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbip.bbipit.core.ui.theme.*
import com.bbip.bbipit.domain.entity.Notification
import kotlinx.coroutines.delay

@Composable
fun NotificationBanner(
    item: Notification,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    // 3.5초 후 자동으로 배너가 사라지게 설정
    LaunchedEffect(key1 = item.id) {
        delay(3500)
        onDismiss()
    }

    // 배너 디자인
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 15.dp)
            .border(
                width = 0.5.dp,
                color = background,
                shape = RoundedCornerShape(50.dp)
            )
            .clickable {
                onClick()
                onDismiss()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아이콘 및 프로필 영역
            Box(contentAlignment = Alignment.BottomEnd) {
                // 프로필 원형 배경
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(primary)
                )

                // 타입별 작은 배지 아이콘
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.type) {
                            "WALKIE" -> Icons.Default.Mic
                            "DM" -> Icons.Default.ChatBubble
                            "REQ" -> Icons.Default.PersonAdd
                            else -> Icons.Default.Mic
                        },
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(15.dp))

            // 텍스트 영역
            Column(
                modifier = Modifier
                    .weight(1f)) {
                Text(
                    text = item.senderName,
                    style = Typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = fontDefault,
                    fontSize = 16.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = when (item.type) {
                        "WALKIE" -> "무전을 보냈습니다"
                        "DM" -> item.content
                        "REQ" -> "친구 요청을 보냈습니다"
                        else -> item.content
                    },
                    style = Typography.bodySmall,
                    color = fontDefault,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
            }

            // 닫기 버튼
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = background,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun NotificationBannerPreview() {
    // 💡 테마 껍데기를 과감히 빼고, 순수 흰색 판때기 위에 배너만 올려서 렌더링을 유도합니다.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC)) // BBIP 라이트 배경색 직접 주입
            .padding(16.dp)
    ) {
        NotificationBanner(
            item = Notification(
                id = "preview_id",
                senderName = "sender",
                type = "WALKIE",
                content = "무전",
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 100000,
                isRead = false,
                roomId = ""
            ),
            onDismiss = {},
            onClick = {}
        )
    }
}