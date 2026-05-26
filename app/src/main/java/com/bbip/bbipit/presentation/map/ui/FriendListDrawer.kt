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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.bbip.bbipit.domain.entity.LiveStatus

@Composable
fun FriendListDrawer(
    friends: List<LiveStatus>,
    selectedFriendUid: String?,
    onFriendClick: (LiveStatus) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mainColor = Color(0xFF956AFC)

    ModalDrawerSheet(
        modifier = modifier.width(280.dp),
        drawerContainerColor = Color.White,
        drawerContentColor = Color(0xFF1E293B)
    ) {
        // 1. HEADER 영역 (배경을 확실히 흰색으로 고정)
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
                        color = Color(0xFF1E293B),
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "클릭 시 해당 위치로 이동합니다",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
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
                        tint = Color(0xFF94A3B8),
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

        // 2. 친구 목록 영역 ★★★ [핵심 변경] ★★★
        // 배경을 연한 회색(0xFFF8FAFC)으로 깔아주어야 흰색 카드가 입체적으로 도드라집니다.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // 카드 간의 간격 확보
        ) {
            items(
                items = friends,
                key = { friend -> friend.uid }
            ) { friend ->
                val isSelected = friend.uid == selectedFriendUid && friend.isSharing

                FriendDrawerItem(
                    friend = friend,
                    isSelected = isSelected,
                    mainColor = mainColor,
                    onClick = { onFriendClick(friend) }
                )
            }
        }

        // 3. FOOTER 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Color(0xFFF1F5F9))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "BBip Radar UI",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1),
                letterSpacing = 1.5.sp
            )
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
    // 선택되었을 때는 보라색 테두리, 아닐 때는 은은한 그림자 경계선 역할을 할 수 있도록 회색 선 부여
    val borderColor = if (isSelected) mainColor.copy(alpha = 0.2f) else Color(0xFFF1F5F9)

    val indicatorColor = when {
        !friend.isSharing -> Color(0xFFF3F3F3)
        friend.isOnline -> Color(0xFFFFFFFF)
        else -> Color(0xFFF3F3F3)
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
            .background(indicatorColor) // 카드는 완전한 순백색
            .border(1.5.dp, indicatorColor, RoundedCornerShape(24.dp))
            .clickable(enabled = friend.isSharing, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 4. 아바타 & 온라인 상태 인디케이터
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
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
            )

            Box(
                modifier = Modifier
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 5. 텍스트 정보 영역
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = friend.nickname,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subText = when {
                !friend.isSharing -> "위치 공유 거부 중"
                !friend.isOnline -> "오프라인"
                else -> "위치 파악 완료" }

            val subTextColor = when {
                !friend.isSharing -> Color(0xFF64748B)
                !friend.isOnline -> Color(0xFF64748B)
                else -> mainColor }

            Text(
                text =  subText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = subTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 6. 우측 상태 레이아웃
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
                    color = mainColor
                )
            }
        } else {
            Text(
                text = "현재",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}