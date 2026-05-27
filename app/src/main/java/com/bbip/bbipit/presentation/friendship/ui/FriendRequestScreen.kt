package com.bbip.bbipit.presentation.friendship.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bbip.bbipit.core.ui.theme.background
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.fontDefault
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.presentation.base.ShowToast
import com.bbip.bbipit.presentation.friendship.viewmodel.FriendRequestViewModel


@Composable
fun FriendRequestScreen(
    navController: NavController,
    viewModel: FriendRequestViewModel = hiltViewModel()
) {

    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val requestList by viewModel.requestList.collectAsStateWithLifecycle()

    // 에러 메시지가 있을 때 토스트 띄우기
    errorMessage?.let { msg ->
        ShowToast(msg)

        // 중요: 토스트를 띄운 직후, ViewModel의 상태를 다시 null로 돌려놔야
        // 화면이 재구성되어도 중복 호출되지 않음
        LaunchedEffect(Unit) {
            viewModel.clearErrorMessage() // ViewModel에 만든 초기화 함수
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp)
    ) {
        // 상단 헤더
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Text("친구 요청 수락", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }


        // 요청 목록
        if (requestList.isEmpty()) {
            // 리스트가 비어있을 때 보여줄 UI
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "친구 요청이 없습니다.",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(requestList) { friend -> // request 대신 friend 사용
                    FriendRequestItem(
                        nickname = friend.nickname,
                        profileImageUrl = friend.profileImageUrl, // 'model' 파라미터가 아니라 정의된 이름 사용
                        onAccept = { viewModel.acceptFriendRequest(friend.uid) }, // request.id -> friend.uid
                        onReject = { viewModel.rejectFriendRequest(friend.uid) }  // request.id -> friend.uid
                    )
                }
            }
        }
    }
}

@Composable
fun FriendRequestItem(
    nickname: String,
    profileImageUrl: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {

    LaunchedEffect(profileImageUrl) {
        android.util.Log.d("FriendRequest", "Item Displayed - Nickname: $nickname, URL: '$profileImageUrl'")
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 이미지 표시
            if (profileImageUrl.isEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "기본 프로필",
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray),
                    tint = Color.DarkGray
                )
            } else {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.size(50.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(nickname, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("친구 요청을 보냈습니다.", fontSize = 12.sp, color = Color.Gray)
            }

            // 버튼들은 Row 안에서 나란히 배치
            TextButton(onClick = onReject) {
                Text("거절", style = Typography.bodyMedium, color = fontDefault)
            }
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("수락", style = Typography.bodyMedium, color = Color.White)
            }
        }
    }
}