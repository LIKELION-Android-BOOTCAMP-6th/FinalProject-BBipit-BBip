package com.bbip.bbipit.presentation.mypage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.presentation.base.BackgroundBox
import androidx.navigation.NavController
import androidx.compose.ui.text.style.TextAlign
import com.bbip.bbipit.core.ui.theme.Typography

@Composable
fun MyPageScreen(
    navController: NavController,
    viewModel: MyPageViewModel = hiltViewModel()
) {

    BackgroundBox(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 상단 헤더
            MyPageHeader()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 2. 프로필 정보 카드 섹션
                item {
                    ProfileCard(viewModel = viewModel)
                }

                // 3. 친구 목록 섹션
                item {
                    FriendsListSection(onAddFriendClick = {
                        viewModel.onAddFriendClicked()
                    }
                    )
                }
            }
        }
        if (viewModel.isAddFriendDialogShowing) {
            AddFriendDialog(
                onDismiss = { viewModel.dismissDialog() },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun MyPageHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "My Page",
            modifier = Modifier.align(Alignment.Center),
            style = Typography.titleLarge
            ,color = primary
        )
        IconButton(
            onClick = { /* 설정 이동 */ },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = primary)
        }
    }
}

@Composable
fun ProfileCard(viewModel: MyPageViewModel) {
    val profile = viewModel.userProfile

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = Color.White.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(4.dp, Color.White)
                ) {
                    /* 이미지 로더 */
                }
                // 편집 버튼
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = primary,
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.padding(8.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "UID: ${profile.uid}", style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { /* 복사 로직 */ }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 기기 정보 칩
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp), tint = primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = profile.deviceModel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun FriendsListSection(onAddFriendClick: () -> Unit) {
    val dummyFriends = List(10) { index -> "Friend ${index + 1}" }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "FRIENDS", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            TextButton(onClick = { onAddFriendClick() }) { // 2. 여기에 함수 연결!
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Friend")
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.5f)
        ) {
            Column {
                dummyFriends.forEachIndexed { index, name ->
                    FriendItem(name)
                    if (index < dummyFriends.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color.LightGray) { }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    viewModel: MyPageViewModel
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 우측 상단 닫기 버튼
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(15.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Add Friend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Enter the UID of the friend you want to add",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // UID 입력창
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.onQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("9823-4812", color = Color.LightGray) },
                    shape = RoundedCornerShape(20.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = primary.copy(alpha = 0.5f))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = Color(0xFFF3E5F5),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Search 버튼
                Button(
                    onClick = { viewModel.searchFriend() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Text("Search", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                // TODO: 검색 결과가 있을 때 여기에 프로필 레이아웃 추가!
            }
        }
    }
}