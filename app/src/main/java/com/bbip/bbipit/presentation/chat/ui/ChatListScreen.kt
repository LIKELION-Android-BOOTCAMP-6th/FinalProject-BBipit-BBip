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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.bbip.bbipit.core.ui.theme.Pink80
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.online
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.core.ui.theme.recording
import com.bbip.bbipit.presentation.base.ConfirmDialog
import kotlinx.coroutines.launch

/**
 * UI State 정의
 */
data class ChatItem(
    val id: String,
    val receiverId: String,
    val senderName: String,
    val lastMessage: String,
    val time: String,
    val isRead: Boolean,
    val unreadCount: Int,
    val isOnline: Boolean,
    val hasImage: Boolean = false,
    val profileImageUrl: String? = null,
    val friendshipStatus: String = ""
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
        viewModel.observeChatRooms()
    }

    Box(modifier = Modifier.fillMaxSize().background(color = background)) {
        Box(modifier = Modifier.fillMaxSize().background(color = background)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.chatList.isEmpty()) {
                // [추가] 채팅 목록이 없을 때 안내 문구
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "채팅 목록이 없습니다.\n새로운 대화를 시작해보세요!",
                        style = Typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp)
                ) {
                    items(uiState.chatList, key = { it.id }) { chatItem ->
                        ChatItemRow( // 이름을 Row로 변경
                            chatItem = chatItem,
                            onClick = { viewModel.onChatItemClicked(chatItem) },
                            onDelete = { /** 로직 추가 **/}
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
                        .height(55.dp),
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
                    style = Typography.bodyLarge,
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
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 스와이프 상태 관리
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            // EndToStart(오른쪽에서 왼쪽) 방향으로 스와이프했을 때만 감지
            if (it == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
            }
            // 2. 핵심: 항상 false를 반환하여 컴포넌트가 스스로 '삭제 상태'로 고정되지 않게 함
            false
        }
    )

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        ConfirmDialog(
            text = "채팅방 나가기",
            semiText = "'${chatItem.senderName}'님과의 대화방을 나가시겠습니까?",
            isSingleBtn = false,
            onDismiss = {
                showDeleteDialog = false
                scope.launch { dismissState.reset() }
            },
            onConfirm = {
                // [주석 처리] 실제 삭제 로직 연결 필요
                // onDelete()

                showDeleteDialog = false
                scope.launch { dismissState.reset() }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false, // 오른쪽으로 미는 동작은 비활성화
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp) // 카드의 위치와 정확히 맞춰주세요
                    .clip(RoundedCornerShape(24.dp))
                    .background(recording), // 삭제 색상 고정
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        },
        content = {
            Card(
                colors = CardDefaults.cardColors(containerColor = background),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 프로필 섹션
                    Box {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                        ) {
                            AsyncImage(
                                model = chatItem.profileImageUrl,
                                contentDescription = "프로필 이미지",
                                error = rememberVectorPainter(image = Icons.Default.Person),
                                modifier = Modifier.fillMaxSize()
                                    .clip(CircleShape)
                                    .border(2.dp, background, CircleShape)
                                    .background(Color.White),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(if (chatItem.isOnline) online else Color.Gray, CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
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
                            text = if (chatItem.hasImage) "📷 사진을 보냈습니다" else chatItem.lastMessage,
                            style = Typography.bodySmall,
                            color = if (chatItem.isRead) Color.Gray else Color.Black,
                            fontWeight = if (chatItem.isRead) FontWeight.Normal else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 시간 및 뱃지 섹션
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = chatItem.time, style = Typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (chatItem.unreadCount > 0) {
                            ChatBadge(count = chatItem.unreadCount)
                        } else {
                            Spacer(modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    )
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
                color = recording,
                shape = CircleShape
            )
            .padding(horizontal = 5.dp, vertical = 2.dp) // 숫자가 늘어나면 옆으로 늘어날 수 있도록
    ) {
        Text(
            text = badgeText,
            style = Typography.labelSmall,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
