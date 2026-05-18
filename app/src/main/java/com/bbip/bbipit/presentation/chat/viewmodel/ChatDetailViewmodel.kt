package com.bbip.bbipit.presentation.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.presentation.chat.ui.ChatDetailUiState
import com.bbip.bbipit.presentation.chat.ui.MessageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.bbip.bbipit.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel // Hilt 어노테이션
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository // 리포지토리 가져오기
) : ViewModel() {

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

            // 리포지토리의 실시간 리스너 Flow를 수집
            chatRepository.observeMessages(roomId).collect { domainMessages ->

                // 도메인 엔티티(ChatMessage) 리스트를 UI용 모델(MessageItem) 리스트로 맵 변환
                val uiMessageItems = domainMessages.map { chatMessage ->
                    MessageItem(
                        id = chatMessage.id,
                        text = chatMessage.content,
                        senderId = chatMessage.senderId,
                        sentAt = chatMessage.sentAt,
                        isRead = chatMessage.isRead,
                        isMine = chatMessage.senderId == myUid, // 내 UID와 비교해서 판단
                        isFailed = false
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        partnerName = "상대방 ($roomId)",
                        partnerStatus = "Active now",
                        friendshipStatus = "ACCEPTED",
                        messages = uiMessageItems, // 변환된 실제 실시간 메시지 리스트
                        errorMessage = null // 성공적으로 로드 시 에러 메시지 초기화
                    )
                }
            }
        }
    }

    /**
     * 실제 메시지 전송 로직 (Callable API 연동 및 실패 대응)
     * 백엔드 스펙에 맞춰 roomId와 receiverId를 파라미터에 추가
     */
    fun sendMessage(roomId: String, receiverId: String, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // 전송 버튼 누르자마자 '전송 중' 상태로 임시 메시지를 화면에 먼저 띄우기
            val tempId = "temp_${System.currentTimeMillis()}"
            val tempMessage = MessageItem(
                id = tempId,
                text = text,
                senderId = myUid,
                sentAt = System.currentTimeMillis(),
                isRead = false,
                isMine = true,
                isFailed = false // 전송 시작 단계에선 일단 실패 아님
            )

            // 로컬 화면 리스트에 임시 메시지 즉시 추가
            _uiState.update {
                it.copy(messages = it.messages + tempMessage)
            }
            // 실제 백엔드 sendMessage Cloud Functions 호출
            val result = chatRepository.sendMessage(roomId, receiverId, text)

            // 결과에 따른 예외 처리 분기문
            when (result) {
                is com.bbip.bbipit.core.result.Result.Success -> {
                    // 성공 시: 어차피 우리가 만든 실시간 리스너(loadChatRoomData)가
                    // DB에 박힌 진짜 데이터를 가져와서 UI를 알아서 새로고침
                    android.util.Log.d("ChatDetailViewModel", "메시지 전송 성공")
                }
                is com.bbip.bbipit.core.result.Result.Failure -> {
                    android.util.Log.e("ChatDetailViewModel", "메시지 전송 실패: ${result.error.message}")

                    // 에러의 원인이 파이어베이스 네트워크 관련인지 체크
                    val errorCause = result.error.cause
                    val isNetworkError = if (errorCause is com.google.firebase.functions.FirebaseFunctionsException) {
                        errorCause.code == com.google.firebase.functions.FirebaseFunctionsException.Code.UNAVAILABLE ||
                                errorCause.code == com.google.firebase.functions.FirebaseFunctionsException.Code.DEADLINE_EXCEEDED
                    } else {
                        // 혹은 기기 자체의 네트워킹 예외(UnknownHostException 등)인지 체크
                        errorCause is java.net.UnknownHostException || errorCause is java.net.ConnectException
                    }

                    // 상황에 맞는 커스텀 에러 문구 세팅
                    val displayMessage = if (isNetworkError) {
                        "네트워크 연결이 불안정합니다. 연결 상태를 확인해 주세요."
                    } else {
                        "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                    }

                    val failedList = _uiState.value.messages.map { msg ->
                        if (msg.id == tempId) msg.copy(isFailed = true) else msg
                    }

                    _uiState.update {
                        it.copy(
                            messages = failedList,
                            errorMessage = displayMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * 💡 [추가된 함수] 채팅방 메시지 읽음 처리 기능 호출
     * @param roomId 읽음 처리할 채팅방 고유 ID
     */
    fun markAsRead(roomId: String) {
        viewModelScope.launch {
            try {
                // 리포지토리를 통해 백엔드의 markMessagesAsRead Callable API 호출
                chatRepository.markMessagesAsRead(roomId)
                android.util.Log.d("ChatDetailViewModel", "읽음 처리 요청 성공")
            } catch (e: Exception) {
                android.util.Log.e("ChatDetailViewModel", "읽음 처리 실패: ${e.message}")
            }
        }
    }

    /**
     * 전송 실패한 임시 메시지를 로컬 UI 리스트에서 제거
     * @param tempId 삭제할 임시 메시지의 고유 ID (temp_...)
     */
    fun removeFailedMessage(tempId: String) {
        _uiState.update { currentState ->
            val updatedMessages = currentState.messages.filterNot { msg -> msg.id == tempId }
            currentState.copy(messages = updatedMessages)
        }
        android.util.Log.d("ChatDetailViewModel", "실패 메시지 삭제 완료: $tempId")
    }

    // 에러 메세지 감시
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}