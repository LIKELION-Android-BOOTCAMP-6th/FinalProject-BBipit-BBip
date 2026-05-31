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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    draggableState: AnchoredDraggableState<DrawerState>, // 💡 상위(MapScreen)에서 상태를 관리하도록 주입받음
    friends: List<WatchLiveStatus>,
    selectedFriendUid: String?,
    onFriendClick: (WatchLiveStatus) -> Unit,
    modifier: Modifier = Modifier,
    drawerWidth: Dp = 150.dp,
    drawerColor: Color = Color(0xFF1E293B)
) {
    val density = LocalDensity.current
    val offsetXDp = with(density) { draggableState.offset.toDp() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(drawerWidth)
            .offset(x = offsetXDp) // 💡 실시간 드래그 오프셋에 맞춰 x축이 -150dp ~ 0dp로 부드럽게 이동
            .background(
                color = drawerColor,
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
            .anchoredDraggable(
                state = draggableState,
                orientation = Orientation.Horizontal
            )
            .padding(vertical = 16.dp, horizontal = 8.dp)
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

            ScalingLazyColumn(
                state = rememberScalingLazyListState(),
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(friends.drop(1), key = { it.uid }) { friend ->
                    val isSelected = friend.uid == selectedFriendUid

                    Button(
                        onClick = { onFriendClick(friend) },
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
                                text = if (friend.isOnline) "온라인" else "오프라인",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (friend.isOnline) Color(0xFF4ADE80) else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}