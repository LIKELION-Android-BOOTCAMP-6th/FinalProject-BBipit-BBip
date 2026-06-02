package com.bbip.bbipit.presentation.notification.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.bbip.bbipit.domain.entity.Notification
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.bbip.bbipit.presentation.base.ConfirmDialog
import com.bbip.bbipit.presentation.base.ShowToast
import com.bbip.bbipit.presentation.base.VoicePlayerViewModel
import com.bbip.bbipit.presentation.notification.viewmodel.NotificationViewModel
import kotlinx.coroutines.delay

// 전체 레이아웃 / 필터링된 리스트 관리 등
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel(),
    voicePlayerViewModel: VoicePlayerViewModel = hiltViewModel()
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(60000) // 1분 대기
            currentTime = System.currentTimeMillis() // 현재 시간 갱신
        }
    }
    val notification by viewModel.notification.collectAsState()
    var selectedFilter by remember { mutableStateOf("전체") }

    val expiredVoiceIds by viewModel.expiredVoiceIds.collectAsState()
    val isReadAllClicked by viewModel.readAllClicked.collectAsState()

    var showExpiredToast by remember { mutableStateOf(false) }

    if (showExpiredToast) {
        ShowToast(message = "만료된 무전은 재생할 수 없습니다.")
        LaunchedEffect(Unit) {
            showExpiredToast = false
        }
    }

    val filteredList by remember(notification, selectedFilter) {
        derivedStateOf {
            val baseList = if (selectedFilter == "전체") notification
            else notification.filter { mapFilterToType(selectedFilter, it.type) }
            baseList.sortedByDescending { it.createdAt }
        }
    }


    // 확인하지 않은 무전이 있는지 체크하는 상태
    var showWalkieDialog by remember { mutableStateOf(false) }

    // 전체확인 다이얼로그
    if (showWalkieDialog) {
        ConfirmDialog(
            text = "확인하지 않은 무전이 있습니다.",
            semiText = "전체 확인 시에도 무전을 들으실 수 있습니다.",
            isSingleBtn = true,
            onDismiss = { },
            onConfirm = {
                showWalkieDialog = false
                viewModel.onReadAllClick()
            }
        )
    }
    Scaffold(
        containerColor = background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding().padding(bottom = 68.dp),
        ) {
            NotificationHeader(
                onReadAll = {
                    val hasUnreadWalkie = notification.any {
                        it.type == "WALKIE" && !it.isRead && !it.isExpired && !expiredVoiceIds.contains(it.id)
                    }
                    if (hasUnreadWalkie) {
                        showWalkieDialog = true
                    } else {
                        viewModel.onReadAllClick()
                    }
                }
            )
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                NotificationFilterBar(
                    selected = selectedFilter,
                    onSelect = { selectedFilter = it },
                    notifications = notification
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            val listState = rememberLazyListState()

            LaunchedEffect(filteredList) {
                if (filteredList.isNotEmpty()) {
                    listState.animateScrollToItem(0)
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "알림이 없습니다.",
                        style = Typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()) {
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val progress = dismissState.progress
                                    val isSwiping =
                                        dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart

                                    if (!isSwiping || progress <= 0f) return@SwipeToDismissBox

                                    val bgAlpha = ((progress - 0.1f) / 0.5f).coerceIn(0f, 0.7f)
                                    val iconAlpha = ((progress - 0.1f) / 0.5f).coerceIn(0f, 1f)

                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                recording,
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
                                    currentTime = currentTime,
                                    readAllClicked = isReadAllClicked,
//                                isVoiceExpiredInUi = expiredVoiceIds.contains(item.id),
                                    onClick = {
                                        if (item.type == "DM") {
                                            Log.d("NotificationScreen", "DM 클릭 - id: ${item.id}")
                                            viewModel.markAsRead(item.id)
                                            navController.navigate(
                                                Routes.ChatRoom(
                                                    roomId = item.roomId,
                                                    receiverId = item.senderId ?: ""
                                                )
                                            )
                                        } else if (item.type == "WALKIE") {
                                            val alreadyExpired = item.isExpired || item.isPlayed
                                            if (alreadyExpired) {
                                                showExpiredToast = true
                                            } else {
                                                viewModel.markAsRead(item.id)
                                                viewModel.onClickAudioNotification(item.id, item.audioId)
                                                navController.navigate(Routes.Map) {
                                                    popUpTo(Routes.Map) { inclusive = false }
                                                    launchSingleTop = true
                                                }
                                            }
                                        } else if (item.type == "REQ") {
                                            viewModel.markAsRead(item.id)
                                            navController.navigate(Routes.FriendRequestList)
                                        } else if (item.type == "ACP") {
                                            viewModel.markAsRead(item.id)
                                            navController.navigate(Routes.FriendList)
                                        }

                                    }
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                thickness = 0.5.dp,
                                color = Color.LightGray.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun NotificationCard(
    item: Notification,
    currentTime: Long,
    onClick: () -> Unit,
    readAllClicked: Boolean = false,
//    isVoiceExpiredInUi: Boolean = false,
    isLocalRead: Boolean = false,
    senderProfileImage: String = ""
) {
    val isWalkieExpired = item.type == "WALKIE" && (item.isExpired || item.isPlayed)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = background
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
                val showDot = !item.isRead && !isWalkieExpired
                if (showDot) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(primary)
                    )
                }
            }

            AsyncImage(
                model = item.profileImage,
                contentDescription = "프로필 이미지",
                error = rememberVectorPainter(image = Icons.Default.Person),
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if(isWalkieExpired) background.copy(alpha = 0.5f) else background,
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                var isMultiLine by remember { mutableStateOf(false) }

                Text(
                    text = item.senderName,
                    style = Typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isWalkieExpired) bottomBarBack else fontDefault,
                    overflow = TextOverflow.Ellipsis
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
                        color = bottomBarBack.copy(alpha = 0.7f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTimestamp(item.createdAt, currentTime),
                    style = Typography.labelSmall,
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isWalkieExpired) {
                    StatusBadge(text = "만료됨", color = bottomBarBack)
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
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
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
}

@Composable
fun NotificationFilterBar(
    selected: String,
    onSelect: (String) -> Unit,
    notifications: List<Notification>
) {
    val filters = listOf(
        FilterItem("전체", Icons.Default.Notifications),
        FilterItem("무전", Icons.Default.Mic),
        FilterItem("DM", Icons.Default.ChatBubble),
        FilterItem("친구 요청", Icons.Default.PersonAdd),
    )

    val hasUnreadMap = mapOf(
        "전체" to notifications.any { !it.isRead },
        "무전" to notifications.any {
            it.type == "WALKIE" && !it.isRead && !it.isPlayed && !it.isExpired
        },
        "DM" to notifications.any { it.type == "DM" && !it.isRead },
        "친구 요청" to notifications.any { (it.type == "REQ" || it.type == "ACP") && !it.isRead }
    )

    Surface(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(27.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        LazyRow(
            modifier = Modifier.fillMaxSize().heightIn(min = 54.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items(filters) { item ->
                val isSelected = selected == item.name
                val hasUnread = hasUnreadMap[item.name] == true
                BadgedBox(
                    badge = {
                        if (hasUnread && !isSelected) {
                            Badge(
                                containerColor = Color.Red,
                                modifier = Modifier
                                    .offset(x = 2.dp, y = 6.dp)
                                    .size(5.dp)
                            )
                        }
                    }
                ) {
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
}

fun formatExpiryTime(expiresAt: Long?, createdAt: Long): String {
    val expireMillis = expiresAt ?: (createdAt + 3 * 60 * 60 * 1000L)
    if (expireMillis == 0L) return ""
    return try {
        val sdf = SimpleDateFormat("~M.dd HH:mm", Locale.KOREA)
        sdf.format(Date(expireMillis))
    } catch (e: Exception) { "" }
}

fun formatTimestamp(createdAt: Long, currentTime: Long): String {
    if (createdAt == 0L) return ""
    val diff = currentTime - createdAt
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
    "친구 요청" -> type == "REQ" || type == "ACP"
    else -> true
}

data class FilterItem(val name: String, val icon: ImageVector)