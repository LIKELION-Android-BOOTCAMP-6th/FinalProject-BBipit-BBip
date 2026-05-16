package com.bbip.bbipit.presentation.base

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bbip.bbipit.core.ui.theme.primary

/**
 * 음성 메시지 수신 시 화면 하단에 표시되는 전역 오버레이 컴포넌트
 */
@Composable
fun VoiceReceptionOverlay(
    viewModel: VoicePlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 수신 상태에 따른 가시성 애니메이션 전환
    AnimatedVisibility(
        visible = uiState.isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(500)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeOut(animationSpec = tween(500)),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
    ) {
        VoiceReceptionCard(
            nickname = uiState.sender?.nickname ?: "알 수 없음",
            profileImageUrl = uiState.sender?.profileImageUrl,
            currentPosition = uiState.currentPosition,
            totalDuration = uiState.currentVoiceMessage?.duration ?: 0,
            onDismiss = { viewModel.dismissMessage() }
        )
    }
}

/**
 * 수신된 음성 메시지 정보 및 재생 상태 표시 카드
 */
@Composable
fun VoiceReceptionCard(
    nickname: String,
    profileImageUrl: String?,
    currentPosition: Int,
    totalDuration: Int,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(45.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 발신자 프로필 이미지 표시
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 발신자 닉네임 표시
                    Text(
                        text = nickname,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color.DarkGray
                    )
                    // 현재 재생 시간 및 총 시간 표시
                    Text(
                        text = "${formatTime(currentPosition)} / ${formatTime(totalDuration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // 재생 상태 시각화를 위한 웨이브폼 영역
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedWaveform()
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // 오버레이 닫기 버튼
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 초 단위를 분:초 형식 문자열로 변환
 */
fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

/**
 * 재생 상태 시각화를 위한 무한 애니메이션 웨이브폼
 */
@Composable
fun AnimatedWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 반복 애니메이션 바 생성
        repeat(25) { index ->
            val duration = remember { (400..800).random() }
            val heightMultiplier by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barHeight"
            )
            // 인덱스 기반 기본 높이 설정
            val baseHeight = when (index % 5) {
                0 -> 10.dp
                1 -> 16.dp
                2 -> 22.dp
                3 -> 18.dp
                else -> 12.dp
            }
            // 그라데이션 적용 애니메이션 바
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(baseHeight * heightMultiplier)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                primary,
                                primary.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
        }
    }
}
