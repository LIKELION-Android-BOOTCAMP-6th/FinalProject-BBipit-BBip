package com.bbip.bbipit.presentation.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.bbip.bbipit.presentation.base.BackgroundBox
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.presentation.chat.viewmodel.ChatDetailViewModel
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Collections
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.primary

/**
 * 채팅방 UI 데이터 모델
 */
data class MessageItem(
    val id: String,          // Firestore 문서 ID (삭제/수정 시 필요)
    val text: String,        // Firestore의 'content'
    val senderId: String,    // Firestore의 'sender_id'
    val sentAt: Long,        // Firestore의 'sent_at' (Timestamp를 Long으로 변환)
    val isRead: Boolean,     // Firestore의 'is_read'
    val isMine: Boolean,     // (senderId == 현재 로그인한 유저 UID)로 판별
    val imageUrl: String? = null
) {
    // UI 표시용 시간 포맷팅 (예: "오후 3:49")
    val time: String get() {
        val date = java.util.Date(sentAt)
        val sdf = java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREA)
        return sdf.format(date)
    }
}

data class ChatDetailUiState(
    val isLoading: Boolean = false,
    val partnerName: String = "",
    val partnerStatus: String = "",
    val messages: List<MessageItem> = emptyList(),
    val friendshipStatus: String = "NONE", // "ACCEPTED", "PENDING", "NONE"
    val errorMessage: String? = null       // 서버 에러(500 등) 발생 시 안내 문구용
)

/**
 * 채팅 상세 화면
 */
@Composable
fun ChatDetailScreen(
    navController: NavController,
    viewModel: ChatDetailViewModel = hiltViewModel() // ViewModel 주입
) {
    // 인자 추출
    val route = navController.currentBackStackEntry?.toRoute<Routes.ChatRoom>()
    val roomId = route?.roomId ?: ""

    val focusManager = LocalFocusManager.current // 포커스 매니저 가져오기

    // roomId가 바뀔 때마다(혹은 화면 진입 시) 데이터 로드
    LaunchedEffect(roomId) {
        viewModel.loadChatRoomData(roomId)
    }

    // UI 상태 구독
    val uiState by viewModel.uiState.collectAsState()

    BackgroundBox(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        // 상태에 따른 화면 분기
        if (uiState.isLoading) {
            // 1. 데이터를 불러오는 중일 때 (중앙에 로딩 표시)
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF6200EE)
            )
        } else {
            // 2. 데이터 로드가 완료되었을 때 실제 UI 표시
            Column(modifier = Modifier.fillMaxSize()) {
                // 상단 바 (ViewModel에서 가져온 실제 파트너 이름 등이 반영됨)
                ChatDetailHeader(navController, uiState)

                // 메시지 영역
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                            })
                        },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DateHeader("TODAY")
                    }
                    items(uiState.messages) { message ->
                        MessageBubble(message)
                    }
                }

                // 하단 입력창 부분
                if (uiState.friendshipStatus != "ACCEPTED") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        color = Color.White.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "대화가 불가능한 상대입니다.",
                            modifier = Modifier.padding(vertical = 18.dp),
                            textAlign = TextAlign.Center,
                            style = Typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                } else {
                    ChatInputArea(onSendClick = { viewModel.sendMessage(it) })
                }
            }
        }
    }
}

@Composable
fun ChatDetailHeader(navController: NavController, state: ChatDetailUiState) {
    // 임시로 온라인 상태라고 가정 (나중에 UiState에 추가)
    val isOnline = true

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.6f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = primary
                )
            }

            // 프로필 이미지 + 상태 표시 점 (Box로 겹치기)
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape, // 동그라미로 수정
                    color = primary
                ) { /* Coil 이미지 로드 영역 */ }

                // 상태 표시 점 (온라인: 초록색, 오프라인: 회색)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (isOnline) Color(0xFF4CAF50) else Color.LightGray,
                            shape = CircleShape
                        )
                        .border(2.dp, Color.White, CircleShape) // 테두리를 줘서 프로필과 구분
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = state.partnerName,
                    style = Typography.bodyLarge,
                    color = primary,
                )
                Text(
                    text = state.partnerStatus,
                    style = Typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageItem) {
    // TODO: 롱클릭 시 클립보드 복사(이모지 유니코드 대응) 로직 추가
    // TODO: message.isFailed가 true일 경우 말풍선 옆에 에러 아이콘 표시

    val alignment = if (message.isMine) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isMine) primary else Color.White.copy(alpha = 0.9f)
    val textColor = if (message.isMine) Color.White else Color.Black

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
        ) {
            // [내 메시지일 때] 시간과 읽음 표시가 말풍선 왼쪽에 위치
            if (message.isMine) {
                MessageStatusSection(message)
                Spacer(modifier = Modifier.width(4.dp))
            }

            Surface(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(
                    topStart = 24.dp, topEnd = 24.dp,
                    bottomStart = if (message.isMine) 24.dp else 4.dp,
                    bottomEnd = if (message.isMine) 4.dp else 24.dp
                ),
                color = bubbleColor
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = Typography.bodyMedium
                )
            }

            // [상대 메시지일 때] 시간과 읽음 표시가 말풍선 오른쪽에 위치
            if (!message.isMine) {
                Spacer(modifier = Modifier.width(4.dp))
                MessageStatusSection(message)
            }
        }
    }
}

@Composable
fun MessageStatusSection(message: MessageItem) {
    Column(horizontalAlignment = Alignment.End) {
        // 읽지 않았을 때만 '1' 표시
        if (!message.isRead) {
            Text(
                text = "1",
                style = Typography.labelSmall,
                color = primary,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = message.time,
            style = Typography.labelSmall.copy(fontSize = 10.sp),
            color = Color.Gray
        )
    }
}

@Composable
fun ChatInputArea(onSendClick: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // OutlinedTextField 적용
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    // heightIn을 제거하거나 min 높이만 설정해서 유연하게 늘어나도록 합니다.
                    .heightIn(min = 54.dp),
                placeholder = {
                    Text("Type a message..", style = Typography.bodySmall)
                },
                shape = RoundedCornerShape(27.dp),

                singleLine = false,   // 줄바꿈 허용
                minLines = 1,         // 최소 1줄 시작
                maxLines = 4,         // 최대 4줄까지 늘어나고 그 이상은 스크롤

                leadingIcon = {
                    // 2. Box로 감싸서 DropdownMenu의 위치를 아이콘 버튼에 고정 ★
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "추가",
                                tint = Color.Gray
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White, RoundedCornerShape(16.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("카메라", style = Typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    /* TODO: 카메라 촬영 로직 호출 */
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("앨범", style = Typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    /* TODO: 앨범 선택 로직 호출 */
                                    expanded = false
                                }
                            )
                        }
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { /* 음성 인식 로직 */ }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "음성",
                            tint = Color.Gray
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.9f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                textStyle = Typography.bodyMedium
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 전송 버튼
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendClick(inputText)
                        inputText = ""

                        keyboardController?.hide() // 전송 후 키보드 내리기
                    }
                },
                modifier = Modifier.size(54.dp),
                containerColor = primary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송")
            }
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = date,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                style = Typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}