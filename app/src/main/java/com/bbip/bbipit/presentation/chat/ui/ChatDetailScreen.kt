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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
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
//    val route = navController.currentBackStackEntry?.toRoute<Routes.ChatRoom>()
//    val roomId = route?.roomId ?: ""
    // 테스트 후 주석 제거 필요

    val roomId = "Wy102dzyw4buC0V6YJuqxjtf6qA2_lNkEvTubtfZJ7WbdZQMw5l5knAc2"
    val receiverId = "lNkEvTubtfZJ7WbdZQMw5l5knAc2"

    val focusManager = LocalFocusManager.current // 포커스 매니저 가져오기
    // UI 상태 구독
    val uiState by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    // uiState.errorMessage가 null이 아닐 때만
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            // 분석된 에러 문구로 토스트 피드백
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()

            // 한 번 띄웠으면 중복으로 안 뜨게 뷰모델 상태 비우기
            viewModel.clearErrorMessage()
        }
    }

    // roomId가 바뀔 때마다(혹은 화면 진입 시) 데이터 로드
    LaunchedEffect(roomId) {
        viewModel.loadChatRoomData(roomId)
        viewModel.markAsRead(roomId)
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // 메시지 개수가 바뀌거나 키보드가 올라와서 레이아웃이 바뀔 때 맨 아래로 스크롤
    // 키보드 높이 실시간 감시
    val imeInsets = WindowInsets.ime
    val keyboardHeight = imeInsets.asPaddingValues().calculateBottomPadding()

    // 즉시(Snap) 맨 아래로 이동하도록 변경
    LaunchedEffect(uiState.messages.size, keyboardHeight) {
        if (uiState.messages.isNotEmpty()) {
            // 외부 코루틴 스케줄러가 개입하기 전에 메인 스레드 안에서 즉시 배치 처리
            listState.scrollToItem(uiState.messages.size - 1)
        }
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
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { DateHeader("TODAY") }
                    items(uiState.messages) { message ->
                        MessageBubble(
                            message = message,
                            onRetrySend = { failedMessage ->
                                // 1. 기존 실패했던 임시 메시지는 화면에서 깔끔하게 지우기
                                viewModel.removeFailedMessage(failedMessage.id)
                                // 2. 똑같은 텍스트로 다시 짱짱하게 전송 요청 날리기
                                viewModel.sendMessage(
                                    roomId = roomId,
                                    receiverId = receiverId,
                                    text = failedMessage.text
                                )
                            },
                            onDeleteClick = { failedMessage ->
                                viewModel.removeFailedMessage(failedMessage.id)
                            }
                        )
                    }
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
                        ChatInputArea(
                            onSendClick = { text ->
                                viewModel.sendMessage(
                                    roomId = roomId,
                                    receiverId = receiverId,
                                    text = text
                                )
                            }
                        )
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
                modifier = Modifier.weight(1f),
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
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageItem,
    onRetrySend: (MessageItem) -> Unit = {}, // 재전송 콜백
    onDeleteClick: (MessageItem) -> Unit = {} // 실패한 메세지 삭제 콜백
) {

    // 토스트를 띄우기 위한 현재 화면의 Context 가져오기
    val context = androidx.compose.ui.platform.LocalContext.current
    // 안드로이드 시스템 클립보드 매니저
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

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
                if (message.isFailed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        // 1. 재전송 버튼
                        IconButton(
                            onClick = { onRetrySend(message) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                                contentDescription = "다시 전송",
                                tint = Color.Red
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // 2. 삭제 버튼
                        IconButton(
                            onClick = { onDeleteClick(message) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "실패 메시지 삭제",
                                tint = Color.Gray
                            )
                        }
                    }
                }

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
                        .width(84.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "복사하기",
                                style = Typography.labelMedium,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = fontDefault,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        onClick = {
                            // 1. 클립보드에 텍스트 복사 실행
                            clipboardManager.setText(
                                androidx.compose.ui.text.AnnotatedString(message.text)
                            )

                            showMenu = false // 메뉴 닫기

                            // 하단 토스트 메시지 띄우기
                            android.widget.Toast.makeText(
                                context,
                                "메시지가 복사되었습니다.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp)
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