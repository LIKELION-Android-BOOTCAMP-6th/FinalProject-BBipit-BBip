package com.bbip.bbipit.presentation.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.presentation.chat.ui.ChatItem
import com.bbip.bbipit.presentation.chat.ui.ChatListUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.functions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Routes>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // 검색어 상태 추가
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 필터링 전 전체 리스트를 저장할 변수
    private var allChatList = listOf<ChatItem>()

    // 파이어베이스 인스턴스 준비
    private val functions = Firebase.functions("asia-northeast3")
    private val db = Firebase.firestore
    private val myUid = Firebase.auth.currentUser?.uid ?: "Wy102dzyw4buC0V6YJuqxjtf6qA2"

    init {
        loadChatList()
    }

    fun loadChatList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 백엔드 Callable 함수 'getMyChatRooms' 호출
                val result = functions
                    .getHttpsCallable("getMyChatRooms")
                    .call()
                    .await()

                // 2. 결과 데이터 파싱
                val data = result.data as? Map<String, Any>
                val roomsList = data?.get("rooms") as? List<Map<String, Any>> ?: emptyList()

                val chatItems = mutableListOf<ChatItem>()

                // 모든 방의 데이터를 동시에(Async) 병렬 조회 요청
                val deferredRooms = roomsList.map { room ->
                    viewModelScope.async {
                        val roomId = room["roomId"] as? String ?: ""
                        val lastMessage = room["lastMessage"] as? String ?: ""
                        val lastMessageAt = (room["lastMessageAt"] as? Number)?.toLong() ?: 0L
                        val participants = room["participants"] as? List<String> ?: emptyList()

                        // 내 UID를 제외한 '상대방 UID' 추출하기
                        val receiverId = participants.firstOrNull { it != myUid } ?: ""

                        // 백엔드가 줄 unreadCounts 맵 객체 파싱
                        var myUnreadCount = 0
                        if (roomId.isNotBlank()) {
                            try {
                                val dmDoc = db.collection("DMs").document(roomId).get().await()
                                if (dmDoc.exists()) {
                                    val unreadCountsMap = dmDoc.get("unread_counts") as? Map<String, Number> ?: emptyMap()
                                    // 내 UID에서 안읽은 개수 쏙 빼오기
                                    myUnreadCount = (unreadCountsMap[myUid] as? Number)?.toInt() ?: 0
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ChatListViewModel", "unread_counts 조회 실패", e)
                            }
                        }

                        // 안 읽은 메시지 개수가 0개 이하면 -> 읽은 방(true), 1개 이상이면 -> 안 읽은 방(false)
                        val isRoomRead = myUnreadCount <= 0

                        // 상대방 이름 조회
                        var partnerName = "알 수 없는 사용자"
                        if (receiverId.isNotBlank()) {
                            try {
                                val userDoc = db.collection("users").document(receiverId).get().await()
                                if (userDoc.exists()) {
                                    partnerName = userDoc.getString("name") ?: "이름 없음"
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ChatListViewModel", "상대방 이름 조회 실패: $receiverId", e)
                            }
                        }


                        // 개별 가공 데이터 리턴
                        ChatItem(
                            id = roomId,
                            senderName = partnerName,
                            lastMessage = lastMessage,
                            time = formatChatTime(lastMessageAt),
                            isRead = isRoomRead,
                            unreadCount = myUnreadCount,
                            isOnline = false,
                            hasImage = false
                        )
                    }
                }

                chatItems.addAll(deferredRooms.awaitAll())

                allChatList = chatItems
                filterChatList(_searchQuery.value)

            } catch (e: Exception) {
                android.util.Log.e("ChatListViewModel", "API 호출 실패", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "채팅 목록을 불러오지 못했습니다."
                    )
                }
            }
        }
    }

    // 검색어 변경 처리
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterChatList(query)
    }

    private fun filterChatList(query: String) {
        val filteredList = if (query.isBlank()) {
            allChatList
        } else {
            allChatList.filter { it.senderName.contains(query, ignoreCase = true) }
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                chatList = filteredList
            )
        }
    }

    fun onChatItemClicked(chatId: String) {
        viewModelScope.launch {
            _navigationEvent.emit(Routes.ChatRoom(roomId = chatId))
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update { it.copy(chatList = allChatList) }
    }

    private fun formatChatTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val date = java.util.Date(timestamp)
        val sdf = java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREA)
        return sdf.format(date)
    }
}