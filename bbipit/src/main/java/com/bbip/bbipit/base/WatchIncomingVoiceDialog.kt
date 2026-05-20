package com.bbip.bbipit.base

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import coil.compose.rememberAsyncImagePainter
import com.bbip.bbipit.models.WatchVoiceData

/**
 * 실시간 음성 메시지 수신 상태를 화면에 표시하는 워치용 팝업 다이얼로그 컴포저블
 */
@Composable
fun WatchIncomingVoiceDialog(
    voiceData: WatchVoiceData,
    onDismiss: () -> Unit
) {
    // 배경 딤 처리 및 워치 해상도 전체 영역 선언을 위한 다이얼로그 컨테이너
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F9).copy(alpha = 0.9f))
                .clickable { onDismiss() }, // 외부 배경 영역 터치 시 다이얼로그 종료 처리
            contentAlignment = Alignment.Center
        ) {
            // 콘텐츠 터치 시 다이얼로그 닫힘 현상 방지를 위한 이벤트 전파 차단 레이어
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {

                // 상단 무전 수신 상태 안내 타이틀 바 영역
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 무전 상태 직관성을 높이기 위한 마이크/스피크 시스템 아이콘 배치
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_btn_speak_now),
                        contentDescription = "Radio Icon",
                        tint = Color(0xFF956AFC),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "무전 수신 중",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF956AFC)
                    )
                }

                // 송신자 프로필 및 재생 상태 표시를 포함하는 중앙 콘텐츠 레이아웃
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    // 고유 브랜드 색상 테두리가 적용된 송신자 프로필 아바타 영역
                    Box(
                        modifier = Modifier
                            .size(80.dp) // 시인성 확대를 고려한 컨테이너 크기 지정
                            .border(2.5.dp, Color(0xFF956AFC), CircleShape) // 브랜드 컬러 외곽선 적용
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val painter = if (voiceData.senderProfileUrl.isNotEmpty()) {
                            rememberAsyncImagePainter(model = voiceData.senderProfileUrl)
                        } else {
                            rememberVectorPainter(image = Icons.Default.Person)
                        }

                        Image(
                            painter = painter,
                            contentDescription = voiceData.senderName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 송신자 닉네임 표시 영역
                    Text(
                        text = voiceData.senderName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 실시간 오디오 재생 상태 시각화를 위한 캡슐형 인디케이터 레이아웃
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.50f)
                            .background(Color(0xFF673AB7).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 오디오 출력 상태 인지를 위한 시스템 볼륨 아이콘 배치
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_lock_silent_mode_off),
                            contentDescription = "Listening",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )

                        // 캡슐 내부 잔여 공간을 모두 활용한 실시간 오디오 파형 그래픽 배치
                        Box(
                            modifier = Modifier
                                .weight(1f), // 아이콘과 가이드 텍스트 간 공간 균형 조절을 위한 가중치 설정
                            contentAlignment = Alignment.Center
                        ) {
                            WatchAnimatedWaveform()
                        }

                        Text(
                            text = "듣는 중...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                // 사용자의 팝업 닫기 동작 유도를 위한 최하단 가이드 힌트 영역
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 스와이프 제스처 시각화를 위한 상단 가이드 바
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "아래로 밀어서 닫기",
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 음성 재생 상태를 직관적으로 표현하는 콤팩트 무한 애니메이션 웨이브폼 컴포저블
 */
@Composable
fun WatchAnimatedWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 소형 디스플레이 해상도를 고려하여 6개의 그래픽 바로 수량 제한 규칙 적용
        repeat(6) { index ->
            // 자연스러운 파형 연출을 위한 바별 무작위 애니메이션 주기 설정
            val duration = remember { (400..700).random() }
            val heightMultiplier by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barHeight"
            )

            // 리듬감 형성을 위한 인덱스별 기본 높이값 차등 할당
            val baseHeight = when (index % 3) {
                0 -> 6.dp
                1 -> 12.dp
                else -> 9.dp
            }

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(baseHeight * heightMultiplier)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White) // 보라색 배경 캡슐과의 대비를 위한 단색 처리
            )
        }
    }
}