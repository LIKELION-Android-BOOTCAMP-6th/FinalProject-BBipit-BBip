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
 * 워치용 실시간 음성 수신 다이얼로그
 */
@Composable
fun WatchIncomingVoiceDialog(
    voiceData: WatchVoiceData,
    onDismiss: () -> Unit
) {
    // 다이얼로그 컨테이너 및 배경 터치 시 닫기 설정
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F9).copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // 내부 콘텐츠 터치 시 이벤트 전파 차단
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {

                // 상단 타이틀 영역
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                // 중앙 프로필 및 재생 상태 레이아웃
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    // 송신자 프로필 이미지 및 테두리 설정
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(2.5.dp, Color(0xFF956AFC), CircleShape)
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

                    // 오디오 재생 상태 인디케이터 레이아웃
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.50f)
                            .background(Color(0xFF673AB7).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_lock_silent_mode_off),
                            contentDescription = "Listening",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )

                        // 실시간 오디오 파형 영역
                        Box(
                            modifier = Modifier
                                .weight(1f),
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

                // 하단 가이드 문구 및 인디케이터 바
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
 * 애니메이션 오디오 파형 컴포저블
 */
@Composable
fun WatchAnimatedWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 총 6개의 파형 바 구성
        repeat(6) { index ->
            // 무작위 애니메이션 주기를 통한 파형 연출
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

            // 인덱스별 기본 높이값 차등 할당
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
                    .background(Color.White)
            )
        }
    }
}