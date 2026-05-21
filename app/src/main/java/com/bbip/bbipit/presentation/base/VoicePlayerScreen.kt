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
 * 실시간 음성 메시지 인입 시 화면 최하단 레이어 오버레이 노출용 전역 재생 바 컴포넌트
 * 수신 뷰모델 가시성 플래그 상태 관찰 기반 스프링 탄성 물리 효과 적용 업다운 슬라이드 애니메이션 수행 목적
 */
@Composable
fun VoicePlayerScreen(
    viewModel: VoicePlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
 * 수신 무전 데이터 발신자 프로필, 재생 트래킹 시간 및 커스텀 오디오 이퀄라이저 그래픽 집약 배치 표출 카드 컴포넌트
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
            // 코일(Coil) 비동기 이미지 로더 컴포넌트 이용 발신자 원형 크롭 프로필 사진 렌더링 처리
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
                    // 가독 시간 포맷팅 헬퍼 함수 경유 현재 재생 진척도 분초 규격 실시간 갱신 표출 처리
                    Text(
                        text = "${formatTime(currentPosition)} / ${formatTime(totalDuration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // 무전 재생 문맥 상태 동적 시각화 목적의 인피니트 애니메이션 웨이브바 패널 배치 영역
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedWaveform()
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // 오버레이 컴포넌트 즉시 숨김 및 재생 상태 초기화용 명시적 닫기 아이콘 버튼
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
 * 정수형 초 단위 수치 데이터 대상 디지털 미디어 플레이어 규격(분:초) 형태 텍스트 패턴 문자열 변환 정렬 헬퍼 함수
 */
fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

/**
 * 무한 루프 트랜지션 명세 기준 개별 세로 막대 배율 팩터 난수 스케일링 가동 실시간 그래픽 웨이브폼 컴포넌트
 */
@Composable
fun AnimatedWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 총 개수 한정 루프 순회 기반 개별 인덱스 주기 부합 고유 진폭 애니메이션 막대 생성 처리
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
            // 자연스러운 주파수 파형 유도 목적의 인덱스 나머지 연산 조건 분기 기준 기본 높이 지정
            val baseHeight = when (index % 5) {
                0 -> 10.dp
                1 -> 16.dp
                2 -> 22.dp
                3 -> 18.dp
                else -> 12.dp
            }
            // 브랜드 고유 기본 색상 및 반투명 알파 채널 색상 배합 버티컬 그라데이션 박스 작도
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