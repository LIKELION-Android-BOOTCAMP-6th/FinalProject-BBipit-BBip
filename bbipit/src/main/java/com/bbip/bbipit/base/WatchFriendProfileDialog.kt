package com.bbip.bbipit.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.wear.compose.material.Text
import coil.compose.rememberAsyncImagePainter
import com.bbip.bbipit.models.WatchLiveStatus
import androidx.compose.ui.window.DialogProperties

/**
 * 워치용 친구 프로필 상세 다이얼로그
 */
@Composable
fun WatchFriendProfileDialog(
    friend: WatchLiveStatus,
    onDismiss: () -> Unit,
    walkieTalkieButton: @Composable () -> Unit
) {
    // 다이얼로그 컨테이너 및 배경 터치 시 닫기 설정
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
//                .padding(top = 5.dp)
                .background(Color(0xFFF3F4F9))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // 내부 콘텐츠 터치 시 이벤트 전파 차단
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false) {}
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // 상하 정렬 레이아웃
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    // 프로필 및 접속 상태 컨테이너
                    Box(modifier = Modifier.wrapContentSize()) {
                        val painter = if (friend.profileImageUrl.isNotEmpty()) {
                            rememberAsyncImagePainter(model = friend.profileImageUrl)
                        } else {
                            rememberVectorPainter(image = Icons.Default.Person)
                        }

                        // 프로필 이미지 및 테두리 설정
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .border(2.5.dp, Color(0xFF956AFC), CircleShape)
                                .padding(2.dp)
                        ) {
                            Image(
                                painter = painter,
                                contentDescription = friend.nickname,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // 실시간 접속 상태 표시 배지 위치 및 색상 설정
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 2.dp, y = 2.dp)
                                .size(16.dp)
                                .background(Color.White, CircleShape)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = if (friend.isOnline) Color(0xFF00E676) else Color(0xFF9E9E9E),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // 닉네임 표시 영역
                    Text(
                        text = friend.nickname,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // 상태 메시지 영역 (최대 1줄, 초과 시 말줄임)
                    Text(
                        text = friend.status.ifEmpty { "상태메세지가 없습니다." },
                        fontSize = 11.sp,
                        color = Color(0xFF956AFC),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    // 외부 주입 무전기 버튼 슬롯
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        walkieTalkieButton()
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 하단 가이드 문구
                    Text(
                        text = "옆으로 밀어서 닫기 ▶",
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}