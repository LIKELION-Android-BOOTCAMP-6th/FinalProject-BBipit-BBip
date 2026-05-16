package com.bbip.bbipit.presentation.test

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

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

    // 리턴값을 화면에 보여주기 위한 상태 변수
    var testResultConsole by remember { mutableStateOf("버튼을 누르면 결과가 여기에 표시됩니다.") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🛠️ Feature 통합 테스트 (UI 리턴 확인)", style = MaterialTheme.typography.headlineMedium)

        // 🖥️ 실시간 리턴값 확인용 결과창 콘솔 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseOnSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("🖥️ 실시간 리턴값 모니터", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 200.dp)
                        .background(Color.Black, shape = MaterialTheme.shapes.small)
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = testResultConsole,
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }

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
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testLogin(email, password)
                    testResultConsole = "testLogin() 리턴:\n-> $res (${if(res) "성공" else "실패"})"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("이메일 로그인")
            }
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testSignUp(email, password)
                    testResultConsole = "testSignUp() 리턴:\n-> $res (${if(res) "성공" else "실패"})"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("이메일 회원가입")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        val user = viewModel.testGetUserProfile(targetUid)
                        testResultConsole = "testGetUserProfile() 리턴:\n-> ${user ?: "실패 혹은 유저 없음"}"
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Text("특정 유저 데이터 조회")
            }
            Button(
                onClick = {
                    scope.launch {
                        val friend = viewModel.fetchFriendProfile(targetUid)
                        testResultConsole = "fetchFriendProfile() 리턴:\n-> ${friend ?: "실패 혹은 친구 아님"}"
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Text("친구 데이터 조회")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testHeartbeat(roomId.ifBlank { null })
                    testResultConsole = "testHeartbeat() 리턴:\n-> $res"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("Heartbeat")
            }
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testFriendRequest(targetUid)
                    testResultConsole = "testFriendRequest() 리턴:\n-> $res"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("친구 요청")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testDeleteFriend(targetUid)
                    testResultConsole = "testDeleteFriend() 리턴:\n-> $res"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("친구 삭제")
            }
            Button(onClick = {
                scope.launch {
                    val friends = viewModel.testGetFriends()
                    testResultConsole = "testGetFriends() 리턴:\n-> " + (friends?.joinToString("\n") { "👤 ${it.nickname}(${it.id}) - 온라인: ${it.isOnline}" } ?: "조회 실패")
                }
            }, modifier = Modifier.weight(1f)) {
                Text("친구 목록")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testUpdateOnlineStatus(true)
                    testResultConsole = "testUpdateOnlineStatus(true) 리턴:\n-> $res"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("온라인 전환")
            }
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testUpdateOnlineStatus(false)
                    testResultConsole = "testUpdateOnlineStatus(false) 리턴:\n-> $res"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("오프라인 전환")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testAcceptFriend(targetUid)
                    testResultConsole = "testAcceptFriend() 리턴:\n-> $res"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("친구 수락")
            }
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testDeclineFriend(targetUid)
                    testResultConsole = "testDeclineFriend() 리턴:\n-> $res"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("친구 거절")
            }
        }
        Button(
            onClick = {
                scope.launch {
                    val res = viewModel.testUpdateProfile(nickname, statusMessage, profileImageUrl)
                    testResultConsole = "testUpdateProfile() 리턴:\n-> $res"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("프로필 업데이트 실행")
        }

        HorizontalDivider()

        Text("2. 채팅 테스트", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testCreateRoom(targetUid)
                    testResultConsole = "testCreateRoom() 리턴:\n-> $res"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("방 생성")
            }
            Button(onClick = {
                scope.launch {
                    val rooms = viewModel.testGetChatRooms()
                    testResultConsole = "testGetChatRooms() 리턴:\n-> " + (rooms?.joinToString("\n") { "🏠 RoomID: ${it.roomId}, LastMsg: ${it.lastMsg}" } ?: "조회 실패")
                }
            }, modifier = Modifier.weight(1f)) {
                Text("방 목록")
            }
        }
        Button(onClick = {
            scope.launch {
                val res = viewModel.testSendMessage(roomId, targetUid, "테스트 메시지")
                testResultConsole = "testSendMessage() 리턴:\n-> $res"
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("메시지 전송")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val res = viewModel.testMarkMessagesRead(roomId)
                    testResultConsole = "testMarkMessagesRead() 리턴:\n-> $res"
                }
            }, modifier = Modifier.weight(1f)) {
                Text("메시지 읽음")
            }
            Button(onClick = {
                scope.launch {
                    testResultConsole = "메시지 구독 시작됨..."
                    viewModel.testObserveMessages(roomId) { messages ->
                        testResultConsole = "💬 실시간 메시지 수신 알림!:\n" + messages.joinToString("\n") { "[${it.senderId}]: ${it.content}" }
                    }
                }
            }, modifier = Modifier.weight(1f)) {
                Text("메시지 수신(구독)")
            }
        }
        Button(
            onClick = {
                scope.launch {
                    val messages = viewModel.testFetchAllMessages(roomId)
                    testResultConsole = "testFetchAllMessages() 리턴:\n-> " + (messages?.joinToString("\n") { "[${it.senderId}]: ${it.content} (읽음:${it.isRead})" } ?: "조회 실패")
                }
            },
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
                    scope.launch {
                        val res = viewModel.testSendVoiceUrl(targetUid, dummyUri)
                        testResultConsole = "testSendVoiceUrl() 리턴:\n-> $res"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("URL 바로 전송")
            }
        }
        Button(onClick = {
            scope.launch {
                testResultConsole = "음성 구독 시작됨..."
                viewModel.testObserveVoice { senderId, url ->
                    testResultConsole = "🎙️ 실시간 음성 수신 알림!:\n발신자: $senderId\nURL: $url"
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("음성 수신(구독)")
        }

        HorizontalDivider()

        Text("4. 알림 테스트", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        val res = viewModel.testMarkNotiRead("single", notiId.ifBlank { null })
                        testResultConsole = "testMarkNotiRead(single) 리턴:\n-> $res"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("알림 읽음(단일)")
            }
            Button(
                onClick = {
                    scope.launch {
                        val res = viewModel.testMarkNotiRead("all")
                        testResultConsole = "testMarkNotiRead(all) 리턴:\n-> $res"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("알림 읽음(전체)")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        val res = viewModel.testDeleteNoti("single", notiId.ifBlank { null })
                        testResultConsole = "testDeleteNoti(single) 리턴:\n-> $res"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("알림 삭제(단일)")
            }
            Button(
                onClick = {
                    scope.launch {
                        val res = viewModel.testDeleteNoti("all")
                        testResultConsole = "testDeleteNoti(all) 리턴:\n-> $res"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("알림 삭제(전체)")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}