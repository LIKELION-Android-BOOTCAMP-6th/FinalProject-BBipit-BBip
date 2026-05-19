package com.bbip.bbipit.presentation.notification

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.*
import com.bbip.bbipit.domain.entity.Notification
import java.text.SimpleDateFormat
import java.util.*

// 전체 레이아웃 / 필터링된 리스트 관리 등
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val notification by viewModel.notification.collectAsState()
    var selectedFilter by remember { mutableStateOf("전체") }

    val expiredVoiceIds by viewModel.expiredVoiceIds.collectAsState()
    val isReadAllClicked by viewModel.readAllClicked.collectAsState()
    val readIds by viewModel.readIds.collectAsState()

    val filteredList by remember(notification, selectedFilter) {
        derivedStateOf {
            val baseList = if (selectedFilter == "전체") notification
            else notification.filter { mapFilterToType(selectedFilter, it.type) }
            baseList.sortedByDescending { it.createdAt }
        }
    }

        Scaffold(
            containerColor = background,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                NotificationHeader(onReadAll = { viewModel.onReadAllClick() })

                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NotificationFilterBar(
                        selected = selectedFilter,
                        onSelect = { selectedFilter = it }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding()
                    )
                ) {
                    items(items = filteredList, key = { it.id }) { item ->
                        @Suppress("DEPRECATION")
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.markAsReadAndDelete(item.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val progress = dismissState.progress
                                val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart

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
                                        .padding(start = 20.dp, end = 20.dp),
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
                            NotificationCard(
                                item = item,
                                readAllClicked = isReadAllClicked,
                                isVoiceExpiredInUi = expiredVoiceIds.contains(item.id),
                                isLocalRead = readIds.contains(item.id),
                                onClick = {
                                    if (item.type == "DM") {
                                        viewModel.markAsRead(item.id)
                                        navController.navigate(Routes.ChatRoom(roomId = item.roomId))
                                    }
                                    else if (item.type == "WALKIE") {
                                        val alreadyExpired = item.isRead || item.isExpired || expiredVoiceIds.contains(item.id)
                                        if (!alreadyExpired) {
                                            viewModel.markAsRead(item.id)
                                            viewModel.setVoiceExpired(item.id)
                                            Toast.makeText(
                                                navController.context,
                                                "무전을 확인합니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    else if (item.type == "REQ") {
                                        viewModel.markAsRead(item.id)
                                        Toast.makeText(
                                            navController.context,
                                            "친구요청을 확인합니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
}

@Composable
fun NotificationCard(
    item: Notification,
    onClick: () -> Unit,
    readAllClicked: Boolean = false,
    isVoiceExpiredInUi: Boolean = false,
    isLocalRead: Boolean = false
) {
    val isWalkieExpired = item.type == "WALKIE" && (item.isRead || item.isExpired || isVoiceExpiredInUi)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWalkieExpired) background.copy(0.5f) else background.copy(0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // 전체 확인 버튼 클릭 여부(readAllClicked) 감지 시 점 제거 사양 적용
                val showDot = !item.isRead && !isWalkieExpired && !readAllClicked && !isLocalRead
                if (showDot) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(primary)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isWalkieExpired) sub1.copy(alpha = 0.5f) else sub1)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                var isMultiLine by remember { mutableStateOf(false) }

                Text(
                    text = item.senderName,
                    style = Typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isWalkieExpired) bottomBarBack else fontDefault
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (item.type) {
                        "WALKIE" -> "무전을 보냈습니다"
                        "DM" -> item.content.take(20)
                        "REQ" -> "친구 요청을 보냈습니다"
                        else -> item.content
                    },
                    style = Typography.bodySmall.copy(
                        fontSize = if (isMultiLine && item.type != "DM") 13.sp else 15.sp,
                        lineHeight = if (isMultiLine && item.type != "DM") 15.sp else 21.sp
                    ),
                    color = if (isWalkieExpired) bottomBarBack else fontDefault,
                    maxLines = if (item.type == "DM") 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        if (textLayoutResult.lineCount >= 2) isMultiLine = true
                    }
                )
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

                if (isWalkieExpired) {
                    StatusBadge(text = "만료됨", color = bottomBarBack.copy(0.5f))
                }
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
            style = Typography.bodySmall,
            fontSize = 11.sp,
            color = background
        )
    }
}

@Composable
fun NotificationHeader(onReadAll: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "알림", style = Typography.bodyLarge)
            Text(
                text = "전체 확인",
                modifier = Modifier.clickable { onReadAll() },
                style = Typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = primary
            )
        }
    }
}

@Composable
fun NotificationFilterBar(selected: String, onSelect: (String) -> Unit) {
    val filters = listOf(
        FilterItem("전체", Icons.Default.Notifications),
        FilterItem("무전", Icons.Default.Mic),
        FilterItem("DM", Icons.Default.ChatBubble),
        FilterItem("친구 요청", Icons.Default.PersonAdd),
    )
    Surface(
        modifier = Modifier.fillMaxWidth().height(54.dp),
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
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
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
                            style = Typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatExpiryTime(expiresAt: Long?, createdAt: Long): String {
    val expireMillis = expiresAt ?: (createdAt + 3 * 60 * 60 * 1000L)
    if (expireMillis == 0L) return ""
    return try {
        val sdf = SimpleDateFormat("~M.dd HH:mm", Locale.KOREA)
        sdf.format(Date(expireMillis))
    } catch (e: Exception) { "" }
}

fun formatTimestamp(createdAt: Long): String {
    if (createdAt == 0L) return ""
    val diff = System.currentTimeMillis() - createdAt
    return when {
        diff < 60000 -> "방금 전"
        diff < 3600000 -> "${diff / 60000}분 전"
        diff < 86400000 -> "${diff / 3600000}시간 전"
        else -> SimpleDateFormat("MM.dd", Locale.KOREA).format(Date(createdAt))
    }
}

fun mapFilterToType(filter: String, type: String): Boolean = when (filter) {
    "무전" -> type == "WALKIE"
    "DM" -> type == "DM"
    "친구 요청" -> type == "FRIEND_ACCEPTED"
    else -> true
}

data class FilterItem(val name: String, val icon: ImageVector)