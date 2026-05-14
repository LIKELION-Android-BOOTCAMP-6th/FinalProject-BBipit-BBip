package com.bbip.bbipit.presentation.noti

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.*
import com.bbip.bbipit.domain.entity.Notifications
import com.bbip.bbipit.presentation.base.BackgroundBox
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// 전체 레이아웃 / 필터링된 리스트 관리 등
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiScreen(
    navController: NavController, viewModel: NotiViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val readAllClicked by viewModel.readAllClicked.collectAsState()
    var selectedFilter by remember { mutableStateOf("전체") }

    val filteredList by remember(notifications, selectedFilter) {
        derivedStateOf {
            if (selectedFilter == "전체") notifications
            else notifications.filter { mapFilterToType(selectedFilter, it.type) }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        BackgroundBox {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                NotiHeader(
                    onReadAll = { viewModel.onReadAllClick() })

                Spacer(modifier = Modifier.height(16.dp))

                NotiFilterBar(
                    selected = selectedFilter, onSelect = { selectedFilter = it })

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items = filteredList, key = { it.notiId }) { item ->

                        // 스와이프 삭제
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.markAsReadAndDelete(item.notiId)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val progress = dismissState.progress
                                val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart

                                // 스와이프 중일 때만 배경
                                if (!isSwiping || progress <= 0f) return@SwipeToDismissBox

                                val bgAlpha = ((progress - 0.1f) / 0.5f).coerceIn(0f, 0.7f)
                                val iconAlpha = ((progress - 0.1f) / 0.5f).coerceIn(0f, 1f)

                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            Color.Red.copy(alpha = bgAlpha),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = iconAlpha)
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false
                        ) {
                            NotiCard(
                                item = item,
                                readAllClicked = readAllClicked,
                                onClick = {
                                    viewModel.markAsRead(item.notiId)
                                    // DM창으로 이동
                                    if (item.type == "DM") {
                                        navController.navigate(Routes.ChatRoom(roomId = item.roomId))
                                    } else if (item.type == "WALKIE" && !item.isExpired) {
                                        Toast.makeText(
                                            navController.context,
                                            "무전을 확인합니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onAcceptFriend = {
                                    viewModel.onAcceptFriendClick(item.notiId)
                                    Toast.makeText(
                                        navController.context,
                                        "친구 요청이 수락되었습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onRejectFriend = {
                                    viewModel.onRejectFriendClick(item.notiId)
                                    Toast.makeText(
                                        navController.context,
                                        "친구 요청이 거절되었습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 개별 알림 내용 // 수락/거절 액션 버튼 등
@Composable
fun NotiCard(
    item: Notifications,
    onClick: () -> Unit,
    onAcceptFriend: () -> Unit,
    onRejectFriend: () -> Unit,
    readAllClicked: Boolean = false
) {
    // 무전은 들었거나(isRead) 3시간 지나면(isExpired) 만료 처리
    val isWalkieExpired = item.type == "WALKIE" && (item.isRead || item.isExpired)
    // 만료된 무전만 흐리게
    val isDimmed = isWalkieExpired
    // 처리 전 친구 요청 클릭 방지
    val isClickable = !(item.type == "REQ" && !item.isRead)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isClickable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDimmed) background.copy(0.5f)
            else background.copy(0.9f)
        ), elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // 읽지 않았고, 만료되지 않은 무전일 때만 보라색 점 표시
                // 전체 확인 클릭 시에도 점 숨김 (isRead는 유지)
                val showDot = !item.isRead && !isWalkieExpired && !readAllClicked
                if (showDot) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(primary)
                    )
                }
            }

            // 프로필 이미지 영역
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDimmed) sub1.copy(alpha = 0.5f)
                        else sub1
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 메시지가 2줄 이상인지 판별
                var isMultiLine by remember { mutableStateOf(false) }

                Text(
                    text = item.senderName,
                    style = Typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDimmed) bottomBarBack else fontDefault
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (item.type) {
                        "WALKIE" -> "무전을 보냈습니다"
                        "DM" -> item.content.take(20)
                        "REQ" -> "님이 친구 요청을 보냈습니다"
                        else -> item.content
                    },
                    // 2줄 이상일 경우
                    style = Typography.bodySmall.copy(
                        fontSize = if (isMultiLine && item.type != "DM") 13.sp else 15.sp,
                        lineHeight = if (isMultiLine && item.type != "DM") 15.sp else 21.sp
                    ),
                    color = if (isDimmed) bottomBarBack else fontDefault,
                    // DM은 1줄 유지
                    maxLines = if (item.type == "DM") 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        if (textLayoutResult.lineCount >= 2) {
                            isMultiLine = true
                        }
                    }
                )
                // WALKIE일 때만 유효시간 표시
                if (item.type == "WALKIE") {
                    Text(
                        text = formatExpiryTime(item.expiresAt, item.createdAt),
                        style = Typography.labelSmall,
                        fontSize = 10.sp,
                        color = bottomBarBack.copy(alpha = 0.7f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTimestamp(item.createdAt),
                    style = Typography.bodySmall,
                    fontSize = 11.sp,
                    color = bottomBarBack
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 타입별 배지 로직
                when {
                    item.type == "REQ" && !item.isRead -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onRejectFriend,
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = fontDefault.copy(0.2f)),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("거절", color = background, fontSize = 12.sp)
                            }
                            Button(
                                onClick = onAcceptFriend,
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primary),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("수락", color = background, fontSize = 12.sp)
                            }
                        }
                    }
                    item.type == "REQ" && item.isRead -> {
                        val badgeText = if (item.content.contains("거절")) "거절됨" else "수락됨"
                        StatusBadge(
                            text = badgeText,
                            color = if (badgeText == "거절됨") bottomBarBack.copy(0.5f) else primary.copy(0.8f)
                        )
                    }
                    // 만료된 무전 배지
                    isWalkieExpired -> {
                        StatusBadge(text = "만료됨", color = bottomBarBack.copy(0.3f))
                    }
                }
            }
        }
    }
}

// 상태 배지 컴포넌트
@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = Typography.bodySmall,
            fontSize = 11.sp,
            color = background
        )
    }
}

@Composable
fun NotiHeader(
    onReadAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "알림",
            style = Typography.bodyLarge
        )
        // 전체 확인 버튼
        Text(
            text = "전체 확인",
            modifier = Modifier.clickable { onReadAll() },
            style = Typography.bodySmall,
            color = primary
        )
    }
}

@Composable
fun NotiFilterBar(
    selected: String, onSelect: (String) -> Unit
) {
    val filters = listOf(
        FilterItem("전체", Icons.Default.Notifications),
        FilterItem("무전", Icons.Default.Mic),
        FilterItem("DM", Icons.Default.ChatBubble),
        FilterItem("친구 요청", Icons.Default.PersonAdd),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(27.dp),
        color = background.copy(0.4f),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items(filters) { item ->
                val isSelected = selected == item.name
                Surface(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelect(item.name) },
                    color = if (isSelected) primary else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) background else primary.copy(0.8f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.name,
                            color = if (isSelected) background else fontDefault.copy(0.7f),
                            style = Typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// 유효시간 텍스트 포맷 (~M.dd HH:mm)
fun formatExpiryTime(expiresAt: Timestamp?, createdAt: Timestamp?): String {
    val expireDate = expiresAt?.toDate()
        ?: createdAt?.toDate()?.let { Date(it.time + 3 * 60 * 60 * 1000L) }
        ?: return ""
    return SimpleDateFormat("~M.dd HH:mm", Locale.KOREA).format(expireDate)
}

// 원래 시간 표시 포맷 (방금 전, n분 전 등)
fun formatTimestamp(timestamp: Timestamp?): String {
    val timeMillis = timestamp?.toDate()?.time ?: return ""
    val diff = System.currentTimeMillis() - timeMillis
    return when {
        diff < 60000 -> "방금 전"
        diff < 3600000 -> "${diff / 60000}분 전"
        diff < 86400000 -> "${diff / 3600000}시간 전"
        else -> SimpleDateFormat("MM.dd", Locale.KOREA).format(Date(timeMillis))
    }
}

// 선택된 필터 카테고리 - 실제 데이터 모델 타입 값 매핑
fun mapFilterToType(filter: String, type: String): Boolean = when (filter) {
    "무전" -> type == "WALKIE"
    "DM" -> type == "DM"
    "친구 요청" -> type == "REQ"
    else -> true
}

// 필터 바의 각 항목을 구성하기 위한 데이터 모델
data class FilterItem(
    val name: String, val icon: ImageVector
)
