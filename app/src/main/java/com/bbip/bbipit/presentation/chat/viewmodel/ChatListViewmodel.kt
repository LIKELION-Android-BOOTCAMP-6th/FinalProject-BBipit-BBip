package com.bbip.bbipit.presentation.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.presentation.chat.ui.ChatItem
import com.bbip.bbipit.presentation.chat.ui.ChatListUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    init {
        loadChatList()
    }

    private fun loadChatList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 데이터 생성 로직 (더미 데이터)
            allChatList = listOf(
                ChatItem("1", "상대방A (1)", "1번 채팅방입니다.", "16:30", true, true),
                ChatItem("2", "상대방B (2)", "2번 채팅방입니다.", "16:25", false, false),
                ChatItem("3", "상대방C (3)", "3번 채팅방입니다.", "13:00", true, true),
                ChatItem("4", "상대방D (4)", "사진을 보냈습니다.", "5월 10일", false, false, hasImage = true),
                ChatItem("5", "상대방E (5)", "5번 채팅방입니다.", "5월 1일", false, false),
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    chatList = allChatList
                )
            }
        }
    }

    // 검색어 변경 처리
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query

        // 검색어가 비어있으면 전체 리스트, 있으면 이름으로 필터링
        val filteredList = if (query.isBlank()) {
            allChatList
        } else {
            allChatList.filter { it.senderName.contains(query, ignoreCase = true) }
        }

        _uiState.update { it.copy(chatList = filteredList) }
    }

    fun onChatItemClicked(chatId: String) {
        viewModelScope.launch {
            _navigationEvent.emit(Routes.ChatRoom(roomId = chatId))
        }
    }

    // 검색 초기화
    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update { it.copy(chatList = allChatList) }
    }
}