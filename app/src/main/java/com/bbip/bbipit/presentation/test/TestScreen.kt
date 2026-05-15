package com.bbip.bbipit.presentation.test

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TestScreen(
    viewModel: FeatureTestViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("test_a@example.com") }
    var password by remember { mutableStateOf("123456") }
    var targetUid by remember { mutableStateOf("") }
    var roomId by remember { mutableStateOf("") }
    var notiId by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("익명") }
    var statusMessage by remember { mutableStateOf("코딩중..") }
    var profileImageUrl by remember { mutableStateOf("https://picsum.photos/200") }
    var dummyUri by remember { mutableStateOf("https://firebasestorage.googleapis.com/v0/b/bbipit.firebasestorage.app/o/Voices%2Ftest%2Ftest.wav?alt=media&token=a551afa5-aee6-4f89-b7ad-24de513f2fff") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🛠️ Feature 통합 테스트", style = MaterialTheme.typography.headlineMedium)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("입력 정보", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("테스트용 이메일") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("테스트용 비밀번호") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetUid,
                    onValueChange = { targetUid = it },
                    label = { Text("상대방 UID (Target)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roomId,
                    onValueChange = { roomId = it },
                    label = { Text("채팅방 ID (RoomId)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notiId,
                    onValueChange = { notiId = it },
                    label = { Text("알림 ID (NotiId - 선택)") },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("프로필 정보", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("닉네임") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = statusMessage,
                    onValueChange = { statusMessage = it },
                    label = { Text("상태 메시지") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = profileImageUrl,
                    onValueChange = { profileImageUrl = it },
                    label = { Text("프로필 이미지 URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HorizontalDivider()

        Text("1. 인증 및 유저", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.testLogin(email, password) }, modifier = Modifier.weight(1f)) {
                Text("로그인")
            }
            Button(onClick = { viewModel.testSignUp(email, password) }, modifier = Modifier.weight(1f)) {
                Text("회원가입")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.testHeartbeat(roomId.ifBlank { null }) }, modifier = Modifier.weight(1f)) {
                Text("Heartbeat")
            }
            Button(onClick = { viewModel.testFriendRequest(targetUid) }, modifier = Modifier.weight(1f)) {
                Text("친구 요청")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.testDeleteFriend(targetUid) }, modifier = Modifier.weight(1f)) {
                Text("친구 삭제")
            }
            Button(onClick = { viewModel.testGetFriends() }, modifier = Modifier.weight(1f)) {
                Text("친구 목록")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.testUpdateOnlineStatus(true) }, modifier = Modifier.weight(1f)) {
                Text("온라인 전환")
            }
            Button(onClick = { viewModel.testUpdateOnlineStatus(false) }, modifier = Modifier.weight(1f)) {
                Text("오프라인 전환")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.testAcceptFriend(targetUid) }, modifier = Modifier.weight(1f)) {
                Text("친구 수락")
            }
            Button(onClick = { viewModel.testDeclineFriend(targetUid) }, modifier = Modifier.weight(1f)) {
                Text("친구 거절")
            }
        }
        Button(
            onClick = { viewModel.testUpdateProfile(nickname, statusMessage, profileImageUrl) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("프로필 업데이트 실행")
        }

        HorizontalDivider()

        Text("2. 채팅 테스트", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.testCreateRoom(targetUid) }, modifier = Modifier.weight(1f)) {
                Text("방 생성")
            }
            Button(onClick = { viewModel.testGetChatRooms() }, modifier = Modifier.weight(1f)) {
                Text("방 목록")
            }
        }
        Button(onClick = { viewModel.testSendMessage(roomId, targetUid, "테스트 메시지") }, modifier = Modifier.fillMaxWidth()) {
            Text("메시지 전송")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.testMarkMessagesRead(roomId) }, modifier = Modifier.weight(1f)) {
                Text("메시지 읽음")
            }
            Button(onClick = { viewModel.testObserveMessages(roomId) }, modifier = Modifier.weight(1f)) {
                Text("메시지 수신(구독)")
            }
        }
        Button(
            onClick = { viewModel.testFetchAllMessages(roomId) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("전체 메시지 내역 조회")
        }

        HorizontalDivider()

        Text("3. 음성 테스트", style = MaterialTheme.typography.titleMedium)
        
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { dummyUri = it.toString() }
        }

        OutlinedTextField(
            value = dummyUri,
            onValueChange = { dummyUri = it },
            label = { Text("음성 파일 URI 또는 URL") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = { launcher.launch("audio/*") }) {
                    Text("파일 선택")
                }
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { 
                    try {
                        viewModel.testSendVoice(targetUid, Uri.parse(dummyUri))
                    } catch(e: Exception) { }
                }, 
                modifier = Modifier.weight(1f),
                enabled = false
            ) {
                Text("업로드 후 전송")
            }
            Button(
                onClick = { viewModel.testSendVoiceUrl(targetUid, dummyUri) },
                modifier = Modifier.weight(1f)
            ) {
                Text("URL 바로 전송")
            }
        }
        Button(onClick = { viewModel.testObserveVoice() }, modifier = Modifier.fillMaxWidth()) {
            Text("음성 수신(구독)")
        }

        HorizontalDivider()

        Text("4. 알림 테스트", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.testMarkNotiRead("single", notiId.ifBlank { null }) },
                modifier = Modifier.weight(1f)
            ) {
                Text("알림 읽음(단일)")
            }
            Button(
                onClick = { viewModel.testMarkNotiRead("all") },
                modifier = Modifier.weight(1f)
            ) {
                Text("알림 읽음(전체)")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.testDeleteNoti("single", notiId.ifBlank { null }) },
                modifier = Modifier.weight(1f)
            ) {
                Text("알림 삭제(단일)")
            }
            Button(
                onClick = { viewModel.testDeleteNoti("all") },
                modifier = Modifier.weight(1f)
            ) {
                Text("알림 삭제(전체)")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "💡 결과는 Android Studio의 [Logcat] 탭에서 'FeatureTest' 태그를 검색하여 확인하세요.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
