package com.bbip.bbipit.presentation.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bbip.bbipit.presentation.chat.viewmodel.ChatListViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.style.TextAlign
import com.bbip.bbipit.core.ui.theme.Pink80
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.primary

/**
 * UI State 정의
 */
data class ChatItem(
    val id: String,
    val senderName: String,
    val lastMessage: String,
    val time: String,
    val isRead: Boolean,
    val unreadCount: Int,
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
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearSearch()
        viewModel.navigationEvent.collect { route ->
            navController.navigate(route)
        }
    }
    LaunchedEffect(Unit) {
        // 상세방에서 백스택으로 돌아올 때마다 목록을 새로 땡겨와서 읽음 상태 갱신
        viewModel.loadChatList()
    }

    Box(modifier = Modifier.fillMaxSize().background(color = background)) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp)
            ) {
                items(uiState.chatList, key = { it.id }) { chatItem ->
                    ChatItemRow( // 이름을 Row로 변경
                        chatItem = chatItem,
                        onClick = { viewModel.onChatItemClicked(chatItem.id) }
                    )
                    // 아이템 사이의 얇은 구분선 추가
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray.copy(alpha = 0.4f)
                    )
                }
            }
        }
        ChatListHeader(viewModel = viewModel)
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
        color = background,
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
                    placeholder = { Text("이름 검색...", style = Typography.bodySmall) },
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
                                modifier = Modifier.size(20.dp),
                                tint = primary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.9f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = Typography.bodyMedium
                )
            } else {
                // [일반 모드] 제목과 검색 아이콘 버튼
                Text(
                    text = "DM",
                    style = Typography.titleLarge,
                    color = primary
                )

                IconButton(
                    onClick = { isSearching = true }, // 클릭 시 검색창으로 변신
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "검색",
                        tint = primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * 개별 채팅 목록 (데이터 연결)
 */
@Composable
fun ChatItemRow(
    chatItem: ChatItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // 클릭 피드백이 들어감
            .padding(horizontal = 20.dp, vertical = 16.dp), // 적절한 터치 영역 확보
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 프로필 섹션
        Box {
            Surface(
                modifier = Modifier.size(56.dp), // 리스트형에 맞춰 살짝 키움
                shape = CircleShape,
                color = Color(0xFFE1BEE7)
            ) { /* TODO: Coil */ }

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

        Spacer(modifier = Modifier.width(16.dp))

        // 텍스트 섹션
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chatItem.senderName,
                style = Typography.bodySmall,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                // DB의 last_message 연결
                text = if (chatItem.hasImage) "📷 사진을 보냈습니다" else chatItem.lastMessage,
                style = Typography.bodySmall,
                color = if (chatItem.isRead) Color.Black else Color.Gray,
                fontWeight = if (chatItem.isRead) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            // 우측 상단: 시간
            Text(
                text = chatItem.time,
                style = Typography.labelSmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp)) // 시간과 뱃지 사이 간격

            // 우측 하단: 안읽음 뱃지
            if (chatItem.unreadCount > 0) {
                ChatBadge(count = chatItem.unreadCount)
            } else {
                // 뱃지가 없을 때 가드 공간 확보
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChatBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return // 안 읽은 메시지가 없으면 아무것도 안 그림
    // 카톡처럼 99개 넘어가면 99+로 보정
    val badgeText = if (count > 99) "99+" else count.toString()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp) // 숫자가 한 자리여도 완벽한 원형 유지
            .background(
                color = Color.Red,
                shape = CircleShape
            )
            .padding(horizontal = 5.dp, vertical = 2.dp) // 숫자가 늘어나면 옆으로 늘어날 수 있도록
    ) {
        Text(
            text = badgeText,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
