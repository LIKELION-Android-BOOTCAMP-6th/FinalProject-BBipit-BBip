package com.bbip.bbipit.presentation.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.bbip.bbipit.presentation.chat.viewmodel.ChatListViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.Close

/**
 * UI State 정의
 */
data class ChatItem(
    val id: String,
    val senderName: String,
    val lastMessage: String,
    val time: String,
    val isUnread: Boolean,
    val isOnline: Boolean,
    val hasImage: Boolean = false,
    val profileImageUrl: String? = null
)

data class ChatListUiState(
    val isLoading: Boolean = false,
    val chatList: List<ChatItem> = emptyList(),
    val errorMessage: String? = null
)

/**
 * DM 목록 화면
 */
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatListViewModel = viewModel()
) {
    // ViewModel의 UiState 구독
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearSearch()

        viewModel.navigationEvent.collect { route ->
            navController.navigate(route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF3E5F5), Color(0xFFE3F2FD))
                )
            )
    ) {
        // UiState의 isLoading 반영
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            // 2. UiState의 chatList 반영
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 100.dp, start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.chatList, key = { it.id }) { chatItem ->
                    ChatItemCard(
                        chatItem = chatItem,
                        onClick = { viewModel.onChatItemClicked(chatItem.id) }
                    )
                }
            }
        }

        ChatListHeader(viewModel = viewModel)

        FloatingActionButton(
            onClick = { /* 새 채팅 작성 */ },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 120.dp),
            containerColor = Color(0xFF6200EE),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Edit, contentDescription = "New Message")
        }
    }
}

/**
 * 상단 헤더
 */
@Composable
fun ChatListHeader(viewModel: ChatListViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    // 검색 모드 활성화 상태 관리
    var isSearching by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.7f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearching) {
                // [검색 모드] 입력창 표시
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    placeholder = { Text("이름 검색...", fontSize = 14.sp) },
                    shape = RoundedCornerShape(26.dp),
                    singleLine = true,
                    // 키보드 옵션을 '검색'으로 설정
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    // 키보드 검색 버튼 클릭 시 다시 일반 헤더로 전환
                    keyboardActions = KeyboardActions(
                        onSearch = { isSearching = false }
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            isSearching = false
                            viewModel.onSearchQueryChanged("") // 검색어 초기화
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.9f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color(0xFF4A148C),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            } else {
                // [일반 모드] 제목과 검색 아이콘 버튼
                Text(
                    text = "DM",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4A148C),
                        letterSpacing = (-0.5).sp
                    )
                )

                IconButton(
                    onClick = { isSearching = true }, // 클릭 시 검색창으로 변신!
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "검색",
                        tint = Color(0xFF4A148C),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * 개별 채팅 카드 (데이터 연결)
 */
@Composable
fun ChatItemCard(
    chatItem: ChatItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        onClick = onClick // 파라미터 연결
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 섹션
            Box {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = Color(0xFFE1BEE7)
                ) {
                    // TODO: Coil 이미지 로더 연결 (chatItem.profileImageUrl)
                }

                // 온라인 상태 점 (isOnline 데이터 기반)
                if (chatItem.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 텍스트 섹션
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chatItem.senderName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = chatItem.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (chatItem.hasImage) "(image) ${chatItem.lastMessage}" else chatItem.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (chatItem.isUnread) Color.Black else Color.Gray,
                    fontWeight = if (chatItem.isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 안 읽은 메시지 표시 (isUnread 데이터 기반)
            if (chatItem.isUnread) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF6200EE), CircleShape)
                )
            }
        }
    }
}