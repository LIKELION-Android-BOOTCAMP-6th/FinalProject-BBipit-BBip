package com.bbip.bbipit.presentation.noti

import android.R.attr.maxLines
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bbip.bbipit.core.ui.theme.*
import com.bbip.bbipit.domain.entity.NotiItem
import com.bbip.bbipit.presentation.base.BackgroundBox
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// 전체 레이아웃 / 필터링된 리스트 관리 등
@Composable
fun NotiScreen(
    navController: NavController, viewModel: NotiViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    var selectedFilter by remember { mutableStateOf("전체") }

    val filteredList by remember(notifications, selectedFilter) {
        derivedStateOf {
            if (selectedFilter == "전체") notifications
            else notifications.filter { mapFilterToType(selectedFilter, it.type) }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Color.Transparent, modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            BackgroundBox(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    NotiHeader(
                        onReadAll = { viewModel.onReadAllClick() })

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    NotiFilterBar(
                        selected = selectedFilter, onSelect = { selectedFilter = it })

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(
                            items = filteredList, key = { it.id }) { item ->
                            NotiCard(item = item, onClick = {
                                viewModel.markAsReadAndDelete(item.id)
                            }, onAcceptFriend = {
                                viewModel.onAcceptFriendClick(item.id)
                            }, onRejectFriend = {
                                viewModel.onRejectFriendClick(item.id)
                            })
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
    item: NotiItem, onClick: () -> Unit, onAcceptFriend: () -> Unit, onRejectFriend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.type != "REQ", onClick = onClick
            ), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(
            containerColor = if (item.isExpired) background.copy(alpha = 0.5f)
            else background.copy(alpha = 0.9f)
        ), elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isExpired) sub1.copy(alpha = 0.5f)
                        else sub1
                    )
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.senderName,
                    style = Typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isExpired) Color.Gray else fontDefault
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                if (item.type == "REQ") {
                    Text(
                        text = "님이 친구 요청을 보냈습니다",
                        style = Typography.bodySmall,
                        color = if (item.isExpired) Color.Gray else fontDefault
                    )
                } else {
                    Text(
                        text = when (item.type) {
                            "WALKIE" -> "무전을 보냈습니다"
                            "DM" -> item.content.take(20)
                            else -> item.content
                        },
                        style = Typography.bodySmall,
                        color = if (item.isExpired) Color.Gray else fontDefault,
                        maxLines = 1
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatTimestamp(item.createdAt),
                    style = Typography.bodySmall,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                if (item.type == "REQ") {
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRejectFriend,
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "거절",
                                color = Color.White,
                                fontSize = 12.sp,
                            )
                        }

                        Button(
                            onClick = onAcceptFriend,
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "수락",
                                color = background,
                                fontSize = 12.sp,
                            )
                        }
                    }
                } else if (item.isExpired) {
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                    Surface(
                        color = sub1.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "만료됨",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = Typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotiHeader(
    onReadAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "알림",
                style = Typography.bodyLarge,
                color = primary,
                fontWeight = FontWeight.Bold
            )
        }
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
        FilterItem("시스템", Icons.Default.Settings)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(27.dp),
        color = sub1.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, background.copy(alpha = 0.5f))
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
                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )
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
    "시스템" -> type == "SYSTEM"
    else -> true
}

// 필터 바의 각 항목을 구성하기 위한 데이터 모델
data class FilterItem(
    val name: String, val icon: ImageVector
)