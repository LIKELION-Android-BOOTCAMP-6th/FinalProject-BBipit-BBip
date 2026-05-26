package com.bbip.bbipit.presentation.friendship.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import coil.compose.AsyncImage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.core.ui.theme.subBackground
import com.bbip.bbipit.presentation.friendship.viewmodel.FriendListViewModel
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.layout.ContentScale
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.online
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.presentation.base.ShowToast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.bbip.bbipit.presentation.base.ConfirmDialog

@Composable
fun FriendListScreen(
    navController: NavController,
    viewModel: FriendListViewModel = hiltViewModel()
) {
    val friendList by viewModel.friendList.collectAsStateWithLifecycle()

    val requestCount by viewModel.requestCount.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current

    // [추가] 화면이 다시 활성화될 때(ON_RESUME)마다 refreshAll() 호출
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showDialog by remember { mutableStateOf(false) }

    var showToastMessage by remember { mutableStateOf<String?>(null) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp)
            .blur(if (showDialog) 10.dp else 0.dp)
    ) {
        // 타이틀 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "친구 목록",
                fontSize = 32.sp,
                style = Typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

            // 친구 추가 버튼
            IconButton(onClick = { showDialog = true }) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "친구 추가",
                    tint = primary
                )
            }
        }

        // 친구 요청 카드
        FriendRequestCard(
            count = requestCount,
            onClick = { navController.navigate(Routes.FriendRequestList) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 친구 목록
        if (friendList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "친구가 없습니다.\n친구 추가를 해보세요!",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(friendList) { friend ->
                    FriendListItem(
                        friend = friend,
                        onMessageClick = {
                            viewModel.createOrGetChatRoom(
                                targetUid = friend.uid,
                                onSuccess = { roomId: String ->
                                    // 여기서 receiverId를 함께 넘겨줍니다.
                                    // ChatRoom 객체가 (roomId: String, receiverId: String)을 받도록 변경되어 있어야 합니다.
                                    navController.navigate(
                                        Routes.ChatRoom(
                                            roomId = roomId,
                                            receiverId = friend.uid
                                        )
                                    )
                                },
                                onError = { errorMessage: String -> // 타입 명시
                                    showToastMessage = errorMessage
                                }
                            )
                            android.util.Log.d("FriendList", "${friend.nickname}님과의 채팅방으로 이동")
                        },
                        onDelete = {
                            viewModel.deleteFriend(
                                targetUid = friend.uid,
                                onSuccess = { message ->
                                    // 서버에서 온 메시지(예: "친구 삭제가 완료되었습니다.")를 토스트로 출력
                                    showToastMessage = message
                                },
                                onError = { errorMessage ->
                                    // 예외 처리 메시지 출력
                                    showToastMessage = errorMessage
                                }
                            )
                            android.util.Log.d("FriendList", "${friend.nickname} 삭제 요청")
                        }
                    )
                }
            }
        }

        showToastMessage?.let {
            ShowToast(it)
            // 한 번 보여준 후 다시 null로 초기화하여 재실행 방지
            showToastMessage = null
        }

        // 다이얼로그 렌더링
        if (showDialog) {
            AddFriendDialog(
                onDismiss = { showDialog = false },
                onConfirm = { uid ->
                    // 💡 ViewModel의 친구 요청 함수 호출
                    viewModel.sendFriendRequest(
                        targetCode = uid,
                        onSuccess = {
                            showDialog = false
                            showToastMessage = "친구 요청을 보냈습니다!"
                        },
                        onError = { errorMessage ->
                            showToastMessage = errorMessage
                            println("친구 요청 실패: $errorMessage")
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun FriendRequestCard(count: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = subBackground, // 연한 보라빛 배경
        modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text("친구 요청", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (count > 0) Color.White else Color.Transparent // 0일 땐 배경 투명 처리
            ) {
                Text(
                    text = if (count > 0) "$count 명의 새로운 요청" else "친구 요청 없음",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = if (count > 0) primary else Color.Gray // 0일 땐 회색 글씨
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun FriendListItem(
    friend: Friend,
    onMessageClick: () -> Unit,
    onDelete: () -> Unit) {
    android.util.Log.d("FriendListDebug", "닉네임: ${friend.nickname}, 상태메세지: '${friend.status}'")

    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 1. 상태를 먼저 생성합니다. (여기선 confirmValueChange를 비워둡니다)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { false } // 자동 삭제 방지
    )

    // 2. 스와이프 상태가 특정 방향으로 끝까지 밀렸는지 감지합니다.
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
            showDeleteDialog = true
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        ConfirmDialog(
            text = "친구 삭제",
            semiText = "'${friend.nickname}'님을 친구 목록에서 삭제하시겠습니까?",
            isSingleBtn = false, // 취소 버튼이 필요하므로 false
            onDismiss = {
                showDeleteDialog = false
                scope.launch { dismissState.reset() }
            },
            onConfirm = {
                onDelete()
                showDeleteDialog = false
                scope.launch { dismissState.reset() }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color),
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
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.clickable {
                    // 카드를 다시 누르면 닫힘
                    scope.launch { dismissState.reset() }
                },
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 프로필 이미지
                    Box(modifier = Modifier.size(50.dp)) {
                        AsyncImage(
                            model = friend.profileImageUrl,
                            contentDescription = "프로필 이미지",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        // 온라인/오프라인 상태 표시
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(subBackground)
                                .padding(2.dp) // 테두리 두께
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (friend.isOnline) online else Color.Gray) // 온라인(녹색), 오프라인(회색)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 닉네임 및 상태 메시지 (Weight를 주어 버튼 공간 확보)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(friend.nickname, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = friend.status.ifBlank { "상태 메시지가 없습니다." },
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    // DM 버튼 추가
                    IconButton(
                        onClick = onMessageClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = "메시지 보내기",
                            tint = primary
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var uid by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        // 다이얼로그 내용 (이미지에서 보신 디자인)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 아이콘 박스
                Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFEDE9FE))) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = primary, modifier = Modifier.align(Alignment.Center))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("친구 추가", style = Typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("친구의 UID를 입력하여\n새로운 인연을 찾아보세요.", color = Color.Gray, textAlign = TextAlign.Center, style = Typography.bodyMedium)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it },
                    placeholder = { Text("UID 입력 (예: 12345678)", style = Typography.bodySmall, fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp),
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row {
                    TextButton(onClick = onDismiss) { Text("취소", style = Typography.bodySmall, color = Color.Gray, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { onConfirm(uid) }, colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                        Text("요청 보내기", style = Typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}