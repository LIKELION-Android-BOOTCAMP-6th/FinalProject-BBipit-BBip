package com.bbip.bbipit.presentation.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.repository.ChatRepository
import com.bbip.bbipit.presentation.chat.ui.ChatItem
import com.bbip.bbipit.presentation.chat.ui.ChatListUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.functions
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Routes>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // 검색어 상태 추가
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 필터링 전 전체 리스트를 저장할 변수
    private var allChatList = listOf<ChatItem>()

    private val db = Firebase.firestore

    private val myUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        observeChatRooms()
    }
    fun observeChatRooms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            chatRepository.observeChatRooms(myUid).collect { chatRooms ->
                try {
                    // 병렬로 개별 채팅방 상세 정보 처리
                    val chatItems = chatRooms.map { room ->
                        viewModelScope.async { processChatRoomDetails(room) }
                    }.awaitAll()

                    allChatList = chatItems
                    filterChatList(_searchQuery.value)
                } catch (e: Exception) {
                    android.util.Log.e("ChatListViewModel", "채팅 목록 처리 중 오류 발생", e)
                    _uiState.update { it.copy(isLoading = false, errorMessage = "채팅 목록을 불러오지 못했습니다.") }
                }
            }
        }
    }

    private suspend fun processChatRoomDetails(room: ChatRoom): ChatItem {
        val roomId = room.id
        val receiverId = room.participants.firstOrNull { it != myUid } ?: ""
        var profileImageUrl: String? = null // 추가

        // 1. unreadCounts 조회 (Firestore DMs 컬렉션)
        var myUnreadCount = 0
        try {
            val dmDoc = db.collection("DMs").document(roomId).get().await()
            if (dmDoc.exists()) {
                val unreadCountsMap = dmDoc.get("unread_counts") as? Map<String, Number> ?: emptyMap()
                myUnreadCount = (unreadCountsMap[myUid] as? Number)?.toInt() ?: 0
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatListViewModel", "unread_counts 조회 실패: $roomId", e)
        }

        // 2. 상대방 이름 조회 (Firestore users 컬렉션)
        var partnerName = "알 수 없는 사용자"
        if (receiverId.isNotBlank()) {
            try {
                val userDoc = db.collection("Users").document(receiverId).get().await()
                if (userDoc.exists()) {
                    partnerName = userDoc.getString("nickname") ?: "이름 없음"
                    profileImageUrl = userDoc.getString("profile_image_url")
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatListViewModel", "상대방 이름 조회 실패: $receiverId", e)
            }
        }

        return ChatItem(
            id = roomId,
            receiverId = receiverId,
            senderName = partnerName,
            profileImageUrl = profileImageUrl,
            lastMessage = room.lastMsg,
            time = formatChatTime(room.updatedAt),
            isRead = myUnreadCount <= 0,
            unreadCount = myUnreadCount,
            isOnline = false,
            hasImage = false
        )
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

    fun onChatItemClicked(chatItem: ChatItem) {
        viewModelScope.launch {
            _navigationEvent.emit(
                Routes.ChatRoom(
                    roomId = chatItem.id,
                    receiverId = chatItem.receiverId
                )
            )
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