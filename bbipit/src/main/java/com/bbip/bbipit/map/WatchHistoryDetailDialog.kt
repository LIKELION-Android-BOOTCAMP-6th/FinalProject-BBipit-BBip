package com.bbip.bbipit.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import coil.compose.rememberAsyncImagePainter
import com.bbip.bbipit.data.WatchHistory

@Composable
fun WatchHistoryDetailDialog(
    history: WatchHistory,
    onDismiss: () -> Unit,
    onOpenOnPhoneClick: () -> Unit
) {
    // usePlatformDefaultWidth = false로 시스템 여백 장벽 해제
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        // 다크 스레이트 톤 전체 화면 도화지 가동
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F9))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween // 상-중-하 영역 분할 배치
        ) {

            // ─── 1. [상단] 기존 발자취 문구를 제거하고 작성자 기록 배지로 대체 ───
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .background(Color(0xFF956AFC).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "👣 ${history.userNickname}님이 기록함", // 요청 반영: 작성자 닉네임 노출
                    fontSize = 10.sp,
                    color = Color(0xFFB493FF),
                    fontWeight = FontWeight.Bold
                )
            }

            // ─── 2. [중앙] 프로필 사진과 바로 밑에 장소명 배치 ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp) // 이미지와 장소명 사이의 최적 여백
            ) {
                // 중앙 집중형 프로필 이미지 아바타
                val profilePainter = if (!history.userProfileImage.isNullOrEmpty()) {
                    rememberAsyncImagePainter(model = history.userProfileImage)
                } else {
                    rememberVectorPainter(image = Icons.Default.Person)
                }

                Box(
                    modifier = Modifier
                        .size(52.dp) // 시선이 집중되는 명확한 크기의 프로필 원
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .border(width = 1.5.dp, color = Color(0xFF956AFC), shape = CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = profilePainter,
                        contentDescription = history.userNickname,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                // 프로필 바로 밑에 장소명 노출
                Text(
                    text = "📍" + history.placeName.ifEmpty { "선택한 장소" },
                    fontSize = 15.sp,
                    color = Color.Black.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ─── 3. [하단] 알약 모양 버튼 및 물리 제스처 방향 안내 가이드 ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 컴팩트 라운드 액션 버튼
                Button(
                    onClick = onOpenOnPhoneClick,
                    modifier = Modifier
                        .fillMaxWidth(0.65f) // 알약 형태 유지를 위한 너비 제약
                        .height(34.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF956AFC) // 시그니처 보라색
                    )
                ) {
                    Text(
                        text = "폰으로 열기",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 요청 반영: WearOS 표준 스와이프 닫기 방향 화살표 교정(▶)
                Text(
                    text = "옆으로 밀어서 닫기  ▶",
                    fontSize = 9.sp,
                    color = Color.Black.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}