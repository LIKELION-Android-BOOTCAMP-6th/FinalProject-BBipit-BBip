package com.bbip.bbipit.base

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import coil.compose.rememberAsyncImagePainter
import com.bbip.bbipit.models.WatchLiveStatus

/**
 * 워치 화면용 친구 프로필 상세 정보 다이얼로그 컴포저블
 */
@Composable
fun WatchFriendProfileDialog(
    friend: WatchLiveStatus,
    onDismiss: () -> Unit,
    walkieTalkieButton: @Composable () -> Unit
) {
    // 배경 딤 처리 및 워치 원형 화면 전체를 채우기 위한 기본 다이얼로그 컨테이너
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp)
                .background(Color(0xFFF3F4F9).copy(alpha = 0.9f))
                .clickable { onDismiss() }, // 외부 배경 터치 시 팝업 닫기 처리
            contentAlignment = Alignment.Center
        ) {
            // 내부 콘텐츠 터치 시 다이얼로그가 닫히지 않도록 이벤트 전파 차단
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false) {}
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // 상하 정렬 기반의 프로필 상세 정보 레이아웃
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    // 프로필 아바타 및 접속 상태 인디케이터 통합 컨테이너
                    Box(modifier = Modifier.wrapContentSize()) {
                        val painter = if (friend.profileImageUrl.isNotEmpty()) {
                            rememberAsyncImagePainter(model = friend.profileImageUrl)
                        } else {
                            rememberVectorPainter(image = Icons.Default.Person)
                        }

                        // 시인성 확보를 위해 확장된 프로필 아바타 및 외곽 보라색 테두리
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .border(2.5.dp, Color(0xFF956AFC), CircleShape) // 고유 브랜드 색상 테두리
                                .padding(2.dp) // 테두리와 이미지 간 간격 설정을 위한 패딩
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

                        // 72dp 아바타 크기에 맞춰 위치가 조정된 실시간 접속 상태 표시 배지
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 2.dp, y = 2.dp) // 아바타 좌상단 경계면 정밀 위치 조정
                                .size(16.dp)
                                .background(Color.White, CircleShape)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = if (friend.isOnline) Color(0xFF00E676) else Color(0xFF9E9E9E), // 온라인(초록)/오프라인(회색) 분기
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // 아바타 크기 확대를 고려하여 축소된 상하 간격 여백
                    Spacer(modifier = Modifier.height(3.dp))

                    // 가독성 향상을 위해 크기가 조정된 친구 닉네임 표시 영역
                    Text(
                        text = friend.nickname,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // 텍스트 초과 시 말줄임표가 적용되는 한 줄 상태 메시지 영역
                    Text(
                        text = friend.status.ifEmpty { "상태메세지가 없습니다." }, // 미설정 시 기본 문구 대체
                        fontSize = 11.sp,
                        color = Color(0xFF956AFC),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth(0.6f) // 가로 해상도 제약을 고려한 최대 폭 제한
                            .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    // 상위 컴포저블에서 주입된 무전기 버튼 배치 전용 슬롯 영역
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 50.dp), // 컴포넌트 규격에 맞춘 고정 크기 할당
                        contentAlignment = Alignment.Center
                    ) {
                        walkieTalkieButton()
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 다이얼로그 종료 방식을 안내하는 하단 가이드 문구
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