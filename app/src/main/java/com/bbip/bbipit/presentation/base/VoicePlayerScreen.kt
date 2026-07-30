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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.bbip.bbipit.core.ui.theme.primary

/**
 * 실시간 음성 메시지 수신 시 화면 하단에 노출되는 전역 재생 바 컴포넌트
 */
@Composable
fun VoicePlayerScreen(
    viewModel: VoicePlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // visible일 때만 Popup이 트리거되도록 처리
    if (uiState.isVisible) {
        Popup(
            alignment = Alignment.BottomCenter, // 화면 하단 정렬
            properties = PopupProperties(
                focusable = false,          // 기존 화면 탭 및 무전 터치 방해 금지
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                clippingEnabled = false,     // 화면 밖으로 잘리지 않게
                usePlatformDefaultWidth = false
            )
        ) {
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
                    nickname = uiState.senderName,
                    profileImageUrl = uiState.senderProfileUrl,
                    currentPosition = uiState.currentPosition,
                    totalDuration = uiState.currentVoiceMessage?.duration ?: 0,
                    onDismiss = { viewModel.dismissMessage() }
                )
            }
        }
    }
}

/**
 * 발신자 프로필, 재생 시간 및 이퀄라이저 파형을 보여주는 수신 카드 컴포넌트
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
            // 발신자 프로필 이미지
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
                    Text(
                        text = nickname,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color.DarkGray
                    )
                    // 현재 재생 시간 및 총 길이 표시
                    Text(
                        text = "${formatTime(currentPosition)} / ${formatTime(totalDuration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // 음성 재생 애니메이션 파형 표시 영역
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedWaveform()
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // 닫기 버튼
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
 * 초 단위 숫자를 분:초 형식의 문자열로 변환하는 함수
 */
fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

/**
 * 실시간 애니메이션이 적용된 오디오 웨이브폼 컴포넌트
 */
@Composable
fun AnimatedWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 반복 루프를 통한 개별 파형 막대 생성
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
            // 인덱스별 기본 높이 설정
            val baseHeight = when (index % 5) {
                0 -> 10.dp
                1 -> 16.dp
                2 -> 22.dp
                3 -> 18.dp
                else -> 12.dp
            }
            // 그라데이션 색상이 적용된 파형 막대 생성
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