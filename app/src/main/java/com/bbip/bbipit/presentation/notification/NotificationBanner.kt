package com.bbip.bbipit.presentation.notification

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbip.bbipit.core.ui.theme.*
import com.bbip.bbipit.domain.entity.Notification
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NotificationBanner(
    item: Notification,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    // 3.5초 후 자동으로 배너가 사라지게 설정
    LaunchedEffect(key1 = item.id) {
        delay(3500)
        onDismiss()
    }

    // 배너 디자인
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .alpha(alpha.value)
            .padding(horizontal = 16.dp, vertical = 15.dp)
            .border(
                width = 0.5.dp,
                color = background,
                shape = RoundedCornerShape(50.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY.value < -100f) {
                            coroutineScope.launch {
                                launch { offsetY.animateTo(-300f) }
                                launch { alpha.animateTo(0f) }
                                onDismiss()
                            }
                        } else {
                            coroutineScope.launch {
                                launch { offsetY.animateTo(0f) }
                                launch { alpha.animateTo(1f) }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (offsetY.value + dragAmount.y <= 0f) {
                            coroutineScope.launch {
                                offsetY.snapTo(offsetY.value + dragAmount.y)
                                val newAlpha = (1f + (offsetY.value / 300f)).coerceIn(0f, 1f)
                                alpha.snapTo(newAlpha)
                            }
                        }
                    }
                )
            }
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
                        .background(sub1)
                        .border(
                            width = 1.5.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                )

                // 타입별 작은 배지 아이콘
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(background)
                        .border(
                            width = 0.5.dp,
                            color = bottomBarBack,
                            shape = CircleShape
                        ),
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
                        modifier = Modifier.size(10.dp)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC))
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