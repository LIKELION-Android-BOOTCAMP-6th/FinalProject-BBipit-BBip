package com.bbip.bbipit.presentation.map.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.online
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.core.ui.theme.recording
import com.bbip.bbipit.domain.entity.LiveStatus

@Composable
fun FriendListDrawer(
    friends: List<LiveStatus>,
    selectedFriendUid: String?,
    isLocationSharing: Boolean,
    onFriendClick: (LiveStatus) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.width(280.dp),
        drawerContainerColor = Color.White,
        drawerContentColor = Color.Gray
    ) {
        // HEADER 영역
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 20.dp, end = 16.dp, top = 32.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "친구 위치 추적",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = Typography.bodyMedium
                    )
                    Text(
                        text = "클릭 시 해당 위치로 이동합니다",
                        fontSize = 10.sp,
                        style = Typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "닫기",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color(0xFFF1F5F9)
        )

        // 친구 목록 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.LightGray.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (!isLocationSharing) {
                // Case 1: 본인이 위치 공유를 꺼둔 경우
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "위치 공유가 꺼져 있습니다",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        style = Typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "나의 실시간 위치 공유를 켜야\n친구들의 위치를 확인할 수 있습니다.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            } else if (friends.isEmpty()) {
                // Case 2: 위치 공유는 켰으나 표시할 친구가 없는 경우
                Text(
                    text = "위치를 공유 중인 친구가 없습니다.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    style = Typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            } else {
                //  Case 3: 정상적으로 친구 리스트 레이아웃 표시
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = friends,
                        key = { friend -> friend.uid }
                    ) { friend ->
                        val isSelected = friend.uid == selectedFriendUid && friend.isSharing

                        FriendDrawerItem(
                            friend = friend,
                            isSelected = isSelected,
                            mainColor = primary,
                            onClick = { onFriendClick(friend) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendDrawerItem(
    friend: LiveStatus,
    isSelected: Boolean,
    mainColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorColor = when {
        !friend.isSharing -> Color(0xFFF3F3F3)
        else -> Color.White
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation =4.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            )
            .clip(RoundedCornerShape(24.dp))
            .background(indicatorColor)
            .border(1.5.dp, indicatorColor, RoundedCornerShape(24.dp))
            .clickable(enabled = friend.isSharing, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아바타 & 온라인 상태 인디케이터
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = friend.profileImageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.dp, Color.LightGray, CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (friend.isOnline) online else Color.Gray)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 텍스트 정보 영역
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = friend.nickname,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                style = Typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text =  friend.status,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                style = Typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        val subText = when {
            friend.isSharing -> "위치 켬"
            else -> "위치 끔"
        }

        val subTextColor = when{
            friend.isSharing -> online
            else -> recording
        }

        // 우측 상태 레이아웃
        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(mainColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "추적중",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = mainColor,
                    style = Typography.bodyMedium
                )
            }
        } else {
            Text(
                text = subText,
                fontSize = 10.sp,
                style = Typography.bodyMedium,
                color = subTextColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}