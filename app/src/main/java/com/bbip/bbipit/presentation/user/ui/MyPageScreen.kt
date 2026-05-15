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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.presentation.base.BackgroundBox
import androidx.navigation.NavController
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
            }
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
