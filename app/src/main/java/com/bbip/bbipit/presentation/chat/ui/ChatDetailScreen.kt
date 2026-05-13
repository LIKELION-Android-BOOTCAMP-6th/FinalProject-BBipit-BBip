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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

/**
 * 채팅방 UI 데이터 모델
 */
data class MessageItem(
    val id: String,
    val text: String = "",
    val imageUrl: String? = null,
    val time: String,
    val isMine: Boolean
)

data class ChatDetailUiState(
    val isLoading: Boolean = false,
    val partnerName: String = "",
    val partnerStatus: String = "",
    val messages: List<MessageItem> = emptyList()
)

/**
 * 채팅 상세 화면
 */
@Composable
fun ChatDetailScreen(
    navController: NavController,
    viewModel: ChatDetailViewModel = viewModel() // ViewModel 주입
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF3E5F5), Color(0xFFE3F2FD))
                )
            )
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

                // 하단 입력창 (ViewModel의 sendMessage 함수와 연결)
                ChatInputArea(onSendClick = { text ->
                    viewModel.sendMessage(text)
                })
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
                    tint = Color(0xFF4A148C)
                )
            }

            // 프로필 이미지 + 상태 표시 점 (Box로 겹치기)
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape, // 동그라미로 수정
                    color = Color(0xFF7E57C2)
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
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A148C),
                    fontSize = 18.sp
                )
                Text(
                    text = state.partnerStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageItem) {
    val alignment = if (message.isMine) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isMine) Color(0xFF6200EE) else Color.White.copy(alpha = 0.9f)
    val textColor = if (message.isMine) Color.White else Color.Black

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 24.dp, topEnd = 24.dp,
                bottomStart = if (message.isMine) 24.dp else 4.dp,
                bottomEnd = if (message.isMine) 4.dp else 24.dp
            ),
            color = bubbleColor,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(2.dp)) {
                // 이미지 메시지가 있을 경우
                if (message.imageUrl != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp), // 텍스트와 겹치지 않게 여백
                        shape = RoundedCornerShape(22.dp),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    ) {
                        // 실제 구현 시 Coil의 AsyncImage를 사용하면 비율 유지가 쉽습니다.
                        /*
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth // 가로를 꽉 채우고 세로는 비율대로
                        )
                        */

                        // 프리뷰 확인용 임시 Box (비율을 위해 aspectRatio 적용)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f) // 시안과 비슷한 세로형 비율(가로:세로 = 7:10)
                                .background(Color.LightGray.copy(alpha = 0.5f))
                        )
                    }
                }

                // 텍스트 메시지가 있을 경우
                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        Text(
            text = message.time,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
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
                    Text("Type a message..", style = MaterialTheme.typography.bodyMedium)
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
                                text = { Text("카메라", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    /* TODO: 카메라 촬영 로직 호출 */
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("앨범", style = MaterialTheme.typography.bodyMedium) },
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
                textStyle = MaterialTheme.typography.bodyMedium
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
                containerColor = Color(0xFF6200EE),
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
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Gray
            )
        }
    }
}