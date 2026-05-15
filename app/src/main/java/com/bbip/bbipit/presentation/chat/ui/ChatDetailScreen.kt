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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.fontDefault
import com.bbip.bbipit.core.ui.theme.primary

/**
 * 채팅방 UI 데이터 모델
 */
data class MessageItem(
    val id: String,
    val text: String,
    val senderId: String,
    val sentAt: Long,
    val isRead: Boolean,
    val isMine: Boolean,
    val isFailed: Boolean = false, // 전송 실패 예외처리를 위한 전송 실패 여부
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
    // UI 상태 구독
    val uiState by viewModel.uiState.collectAsState()

    // roomId가 바뀔 때마다(혹은 화면 진입 시) 데이터 로드
    LaunchedEffect(roomId) {
        viewModel.loadChatRoomData(roomId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = com.bbip.bbipit.core.ui.theme.background
    ) {
        if (uiState.isLoading) {
            // 로딩 UI
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // 상단 바
                ChatDetailHeader(navController, uiState)

                // 메시지 영역
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { DateHeader("TODAY") }
                    items(uiState.messages) { message -> MessageBubble(message) }
                }

                // 입력 세션
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding() // 키보드 높이만큼 패딩 부여
                        .navigationBarsPadding() // 시스템 하단 바 공간 확보
                ) {
                    if (uiState.friendshipStatus != "ACCEPTED") {
                        // 대화 불가능 Surface (기존 코드 동일)
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            // 뒤로가기 버튼
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = primary
                )
            }

            // 2. 중앙: 프로필 및 이름 정보 (weight를 주어 공간을 꽉 채우게 함)
            Row(
                modifier = Modifier.weight(1f), // ✅ 중요: 이 weight가 있어야 나가기 버튼이 오른쪽 끝으로 밀림
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = primary
                    ) { /* Coil */ }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isOnline) Color(0xFF4CAF50) else Color.LightGray,
                                shape = CircleShape
                            )
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = state.partnerName,
                        style = Typography.bodyLarge,
                        fontSize = 20.sp,
                        color = fontDefault,
                    )
                    Text(
                        text = state.partnerStatus,
                        style = Typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // 3. 오른쪽 끝: 채팅방 나가기 버튼 추가
            IconButton(onClick = {
                /* TODO: 채팅방 나가기 다이얼로그 또는 로직 연결 */
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "나가기",
                    tint = Color.Red
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

    var showMenu by remember { mutableStateOf(false) }

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

            Box {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        // 롱클릭 감지 인터랙션 추가
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { showMenu = true }
                            )
                        },
                    shape = RoundedCornerShape(
                        topStart = 24.dp, topEnd = 24.dp,
                        bottomStart = if (message.isMine) 24.dp else 4.dp,
                        bottomEnd = if (message.isMine) 4.dp else 24.dp
                    ),
                    color = bubbleColor,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = message.text,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = Typography.bodyMedium
                    )
                }

                // 롱클릭 시 나타날 메뉴
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .width(100.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "복사하기",
                                style = Typography.bodySmall,
                                color = fontDefault,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        onClick = {
                            showMenu = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    )
                }
            }

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
                    // heightIn을 제거하거나 min 높이만 설정해서 유연하게 늘어나도록
                    .heightIn(min = 54.dp),
                placeholder = {
                    Text("메세지를 입력하세요...", style = Typography.bodySmall)
                },
                shape = RoundedCornerShape(27.dp),

                singleLine = false,   // 줄바꿈 허용
                minLines = 1,         // 최소 1줄 시작
                maxLines = 4,         // 최대 4줄까지 늘어나고 그 이상은 스크롤

                leadingIcon = {
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
                            modifier = Modifier.background(Color.White)
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