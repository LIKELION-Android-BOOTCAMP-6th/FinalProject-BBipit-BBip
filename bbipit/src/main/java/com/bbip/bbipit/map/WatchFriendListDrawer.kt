package com.bbip.bbipit.map

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import com.bbip.bbipit.models.WatchLiveStatus

// 드로어 상태 정의
enum class DrawerState {
    CLOSED, OPENED
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WatchFriendListDrawer(
    draggableState: AnchoredDraggableState<DrawerState>,
    friends: List<WatchLiveStatus>,
    selectedFriendUid: String?,
    onFriendClick: (WatchLiveStatus) -> Unit,
    modifier: Modifier = Modifier,
    drawerWidth: Dp = 150.dp,
    drawerColor: Color = Color(0xFF1E293B)
) {
    val density = LocalDensity.current
    val offsetXDp = with(density) { draggableState.offset.toDp() }

    // 0번째 인덱스는 항상 '나'의 상태
    val myStatus = friends.firstOrNull()
    val isMeSharing = myStatus?.isSharing ?: true // 내가 위치 공유 중인지 여부
    val actualFriends = friends.drop(1)           // 나를 제외한 실제 친구들 목록

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(drawerWidth)
            .offset(x = offsetXDp)
            .background(
                color = drawerColor,
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
            .anchoredDraggable(
                state = draggableState,
                orientation = Orientation.Horizontal
            )
            .padding(vertical = 16.dp, horizontal = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "위치 추적",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when {
                // Case 1: 본인이 위치 공유를 꺼둔 경우
                !isMeSharing -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "위치 공유 꺼짐",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF43F5E),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "나의 공유를 켜야\n친구들을 봅니다.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Case 2: 위치 공유는 켰으나 표시할 친구가 없는 경우
                actualFriends.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "공유 중인\n친구 없음",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Case 3: 정상 상태
                else -> {
                    ScalingLazyColumn(
                        state = rememberScalingLazyListState(),
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(actualFriends, key = { it.uid }) { friend ->
                            val isSelected = friend.uid == selectedFriendUid
                            val isSharing = friend.isSharing

                            Button(
                                onClick = { onFriendClick(friend) },
                                enabled = isSharing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF956AFC) else Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = friend.nickname,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = if (isSharing) {
                                            if (friend.isOnline) "온라인" else "오프라인"
                                        } else {
                                            "위치 꺼짐"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSharing) {
                                            if (friend.isOnline) Color(0xFF4ADE80) else Color.Gray
                                        } else {
                                            Color(0xFFF43F5E)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}