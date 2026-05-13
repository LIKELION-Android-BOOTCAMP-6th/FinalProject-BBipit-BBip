package com.bbip.bbipit.presentation.noti

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bbip.bbipit.core.ui.theme.* // 💡 정의하신 컬러 변수들을 가져옵/니다.
import com.bbip.bbipit.domain.entity.NotiItem
import com.bbip.bbipit.presentation.base.BackgroundBox
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotiScreen(
    navController: NavController,
    viewModel: NotiViewModel = hiltViewModel()
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
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
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
                        onReadAll = { viewModel.onReadAllClick() }
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    NotiFilterBar(
                        selected = selectedFilter,
                        onSelect = { selectedFilter = it }
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(
                            items = filteredList,
                            key = { it.id }
                        ) { item ->
                            NotiCard(
                                item = item,
                                onClick = {
                                    viewModel.markAsReadAndDelete(item.id)
                                },
                                onAcceptFriend = {
                                    viewModel.onAcceptFriendClick(item.id)
                                },
                                onRejectFriend = {
                                    viewModel.onRejectFriendClick(item.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotiCard(
    item: NotiItem,
    onClick: () -> Unit,
    onAcceptFriend: () -> Unit,
    onRejectFriend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.type != "REQ",
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            // 💡 배경색 테마 반영 (background 컬러 사용)
            containerColor = if (item.isExpired) background.copy(alpha = 0.5f)
            else background.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        // 💡 sub1 컬러를 프로필 배경으로 활용
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
                    // 💡 fontDefault 컬러 적용
                    color = if (item.isExpired) Color.Gray else fontDefault
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                if (item.type == "REQ") {
                    Text(
                        text = "님이 친구 요청을 보냈습니다",
                        style = Typography.bodySmall,
                        color = if (item.isExpired) Color.Gray else fontDefault.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = when (item.type) {
                            "WALKIE" -> "무전을 보냈습니다"
                            "DM" -> item.content.take(20)
                            else -> item.content
                        },
                        style = Typography.bodySmall,
                        color = if (item.isExpired) Color.Gray else fontDefault.copy(alpha = 0.7f),
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
                            colors = ButtonDefaults.buttonColors(
                                // 💡 거절 버튼: PurpleGrey40 스타일 참고하여 회색 계열 적용
                                containerColor = Color.LightGray,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "거절",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onAcceptFriend,
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                // 💡 수락 버튼: primary 컬러 적용
                                containerColor = primary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "수락",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (item.isExpired) {
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                    Surface(
                        color = sub1.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
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
fun NotiFilterBar(
    selected: String,
    onSelect: (String) -> Unit
) {
    val filters = listOf(
        FilterItem("전체", Icons.Default.AllInclusive),
        FilterItem("무전", Icons.Default.Mic),
        FilterItem("DM", Icons.Default.ChatBubble),
        FilterItem("친구 요청", Icons.Default.PersonAdd)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(27.dp),
        // 💡 필터바 배경을 sub1의 투명도 버전으로 변경
        color = sub1.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
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
                    // 💡 선택 시 primary 컬러 적용
                    color = if (isSelected) primary else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else primary
                        )
                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )
                        Text(
                            text = item.name,
                            color = if (isSelected) Color.White else fontDefault,
                            style = Typography.bodySmall
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
                // 💡 제목에 fontDefault 적용
                color = fontDefault,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "전체 읽음",
                modifier = Modifier.clickable { onReadAll() },
                style = Typography.bodySmall,
                color = primary // 💡 액션 텍스트에 primary 적용
            )
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

fun mapFilterToType(filter: String, type: String): Boolean = when (filter) {
    "무전" -> type == "WALKIE"
    "DM" -> type == "DM"
    "친구 요청" -> type == "REQ"
    else -> true
}

data class FilterItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)