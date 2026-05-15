package com.bbip.bbipit.presentation.chat.viewmodel

import android.os.Process.myUid
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.presentation.chat.ui.ChatDetailUiState
import com.bbip.bbipit.presentation.chat.ui.MessageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatDetailViewModel : ViewModel() {

    private val myUid: String = "Wy102dzyw4buC0V6YJuqxjtf6qA2"
    // UI 상태 관리
    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    /**
     * 채팅방 데이터 로드
     * @param roomId Navigation에서 넘겨받은 채팅방 고유 ID
     */
    fun loadChatRoomData(roomId: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 가상의 데이터 로드 (Firestore 데이터 형식 반영)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    partnerName = "상대방 ($roomId)",
                    partnerStatus = "Active now",
                    friendshipStatus = "ACCEPTED", // 대화 가능 상태로 설정
                    messages = listOf(
                        MessageItem(
                            id = "1",
                            text = "반가워요! $roomId 번 방입니다.",
                            senderId = "other_user_id",
                            sentAt = System.currentTimeMillis() - 100000,
                            isRead = true,
                            isMine = false
                        ),
                        MessageItem(
                            id = "2",
                            text = "데이터 연동 테스트 중이에요.",
                            senderId = myUid,
                            sentAt = System.currentTimeMillis() - 50000,
                            isRead = true,
                            isMine = true
                        )
                    )
                )
            }
        }
    }

    /**
     * 메시지 전송 로직
     */
    fun sendMessage(text: String) {
        // TODO: Firestore 전송 실패 시 이벤트를 수집하여 "네트워크 연결 확인" 토스트 발생 로직 추가

        if (text.isBlank()) return

        viewModelScope.launch {
            // Firestore에 저장될 구조와 동일하게 생성
            val newMessage = MessageItem(
                id = System.currentTimeMillis().toString(),
                text = text,
                senderId = myUid,
                sentAt = System.currentTimeMillis(),
                isRead = false, // 새로 보낸 메시지는 아직 안 읽음 상태
                isMine = true
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + newMessage
                )
            }
        }
    }
}