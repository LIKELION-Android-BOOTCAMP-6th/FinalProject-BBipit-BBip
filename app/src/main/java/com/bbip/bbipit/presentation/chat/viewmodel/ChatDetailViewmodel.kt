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
import com.bbip.bbipit.domain.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel // Hilt 어노테이션
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository, // 리포지토리 가져오기
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val friendRepository: FriendRepository,
    private val lifeCyclerManager: com.bbip.bbipit.core.base.LifeCycleManager
) : ViewModel() {

    // 현재 접속 중인 방 ID를 저장 (서버가 읽음 처리를 위해 사용)
    private var currentRoomId: String? = null

    private var roomUpdateJob: kotlinx.coroutines.Job? = null

    private val myUid: String
        get() = auth.currentUser?.uid ?: ""

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

            val uids = roomId.split("_")
            val partnerUid = uids.firstOrNull { it != myUid } ?: uids.last()

            // 1. 친구 정보(프로필 + 접속 상태) 실시간 구독
            // myFriends Flow 하나만 있으면 모든 정보가 들어옵니다!
            launch {
                friendRepository.myFriends.collect { friendsList ->
                    val partner = friendsList.find { it.uid == partnerUid }
                    if (partner != null) {
                        _uiState.update {
                            it.copy(
                                partnerName = partner.nickname,       // 💡 존재해야 함
                                partnerImageUrl = partner.profileImageUrl, // 💡 존재해야 함
                                partnerStatus = if (partner.isOnline) "온라인" else "오프라인" // 💡 존재해야 함
                            )
                        }
                    }
                }
            }

            // 2. 메시지 실시간 구독
            launch {
                chatRepository.observeMessages(roomId).collect { domainMessages ->
                    // 1. 도메인 메시지를 MessageItem으로 먼저 변환
                    val serverMessages = domainMessages.map { chatMessage ->
                        MessageItem(
                            id = chatMessage.id,
                            text = chatMessage.content,
                            senderId = chatMessage.senderId,
                            sentAt = chatMessage.sentAt,
                            isRead = chatMessage.isRead,
                            isMine = chatMessage.senderId == myUid
                        )
                    }

                    // 💡 1. 서버 메시지 ID 목록을 가져옵니다.
                    val serverIds = serverMessages.map { it.id }.toSet()

                    // 💡 2. 내가 방금 만든 임시 메시지(isFailed == true인 것) 중에서
                    // 서버에 아직 안 들어간(ID가 서버 리스트에 없는) 것만 골라냅니다.
                    val persistentFailedMessages = _uiState.value.messages.filter {
                        it.isFailed && it.id !in serverIds
                    }

                    // 💡 3. 합치기 (서버 데이터 + 내 실패 데이터)
                    val allMessages = (serverMessages + persistentFailedMessages).sortedBy { it.sentAt }

                    // 2. 변환된 아이템들을 날짜별로 그룹화
                    val grouped = allMessages.groupBy { message ->
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA)
                        sdf.format(java.util.Date(message.sentAt))
                    }

                    // 내가 지금 이 방을 보고 있다면
                    if (currentRoomId == roomId && domainMessages.any { !it.isRead }) {
                        markAsRead(roomId)
                    }

                    // 3. UI 상태 업데이트
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = allMessages,      // 전체 메시지 리스트도 유지 (스크롤 위치 계산용)
                            groupedMessages = grouped,   // 💡 그룹화된 데이터 전달
                            friendshipStatus = "ACCEPTED",
                            errorMessage = null
                        )
                    }
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
            _uiState.update { currentState ->
                val newList = currentState.messages.toMutableList()
                newList.add(tempMessage)
                currentState.copy(messages = newList.toList())
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

                    _uiState.update { currentState ->
                        val updatedMessages = currentState.messages.map { msg ->
                            if (msg.id == tempId) msg.copy(isFailed = true) else msg
                        }

                        val newGrouped = updatedMessages.groupBy { message ->
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA)
                            sdf.format(java.util.Date(message.sentAt))
                        }

                        currentState.copy(
                            messages = updatedMessages,
                            groupedMessages = newGrouped, // 💡 UI가 변경된 groupedMessages를 받음
                            errorMessage = displayMessage
                        )
                    }
                }
            }
        }
    }

    fun updateCurrentRoom(roomId: String?) {
        lifeCyclerManager.updateCurrentRoom(roomId)

        roomUpdateJob?.cancel()

        roomUpdateJob = viewModelScope.launch {
            try {
                // 서버 통신 수행
                chatRepository.updateActiveRoom(myUid, roomId)
                android.util.Log.d("ChatDetailViewModel", "현재 방 상태 업데이트: $roomId")
            } catch (e: Exception) {
                // 이 블록 안에서 e를 검사합니다.
                if (e is kotlinx.coroutines.CancellationException) {
                    // 취소된 경우: 아무것도 하지 않음 (로그를 남기지 않음)
                } else {
                    // 진짜 에러인 경우: 로그 출력
                    android.util.Log.e("ChatDetailViewModel", "방 상태 업데이트 실패", e)
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
            // 기존 리스트에서 특정 ID만 제외하고, .toList()를 호출해 새 객체로 만듦
            val updatedMessages = currentState.messages.filterNot { it.id == tempId }.toList()

            // 이렇게 하면 currentState.copy(...)를 통해 새로운 상태 객체가 생성됨
            currentState.copy(messages = updatedMessages)
        }
        android.util.Log.d("ChatDetailViewModel", "삭제 완료: $tempId")
    }

    // 에러 메세지 감시
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

}