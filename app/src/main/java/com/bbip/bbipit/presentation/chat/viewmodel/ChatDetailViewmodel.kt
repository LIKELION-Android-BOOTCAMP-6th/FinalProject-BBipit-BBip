package com.bbip.bbipit.presentation.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.presentation.chat.ui.ChatDetailUiState
import com.bbip.bbipit.presentation.chat.ui.MessageItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatDetailViewModel : ViewModel() {

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

            _uiState.update {
                it.copy(
                    isLoading = false,
                    partnerName = "상대방 ($roomId)",
                    partnerStatus = "Active now",
                    messages = listOf(
                        MessageItem(
                            id = "1",
                            text = "반가워요! $roomId 번 방입니다.",
                            time = "10:00 AM",
                            isMine = false
                        ),
                        MessageItem(
                            id = "2",
                            text = "데이터 연동 테스트 중이에요.",
                            time = "10:01 AM",
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
        if (text.isBlank()) return

        viewModelScope.launch {
            val currentTime = java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREA).format(java.util.Date())
            val newMessage = MessageItem(
                id = System.currentTimeMillis().toString(),
                text = text,
                time = currentTime,
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