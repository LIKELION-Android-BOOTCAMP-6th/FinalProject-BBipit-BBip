package com.bbip.bbipit.presentation.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.domain.entity.ChatRoom
import com.bbip.bbipit.domain.repository.ChatRepository
import com.bbip.bbipit.domain.repository.FriendRepository
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


// 내부에 정보 저장용 데이터 클래스
data class UserInfo(
    val name: String,
    val profileImageUrl: String?,
    val isOnline: Boolean,
    val friendshipStatus: String)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState = _uiState.asStateFlow()

    // 사용자 상태
    private val userInfos = mutableMapOf<String, UserInfo>()
    private val userListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

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
        observeFriends() // 💡 친구 목록 구독 시작
    }

    private fun observeFriends() {
        viewModelScope.launch {
            // FriendRepository에서 실시간으로 갱신되는 리스트 구독
            friendRepository.myFriends.collect { friendsList ->

                friendsList.forEach {
                    android.util.Log.d("ChatListViewModel", "UID: ${it.uid}, 상태: ${it.friendshipStatus}")
                }
                // friendsList가 바뀔 때마다 userInfos 맵을 갱신
                friendsList.forEach { friend ->
                    userInfos[friend.uid] = UserInfo(
                        name = friend.nickname,
                        profileImageUrl = friend.profileImageUrl,
                        isOnline = friend.isOnline,
                        friendshipStatus = friend.friendshipStatus
                    )
                }
                // 데이터 갱신 후 UI 리프레시
                refreshChatList()
            }
        }
    }

    private fun refreshChatList() {
        // [수정] allChatList를 아예 최신 정보로 교체합니다.
        allChatList = allChatList.map { item ->
            val info = userInfos[item.receiverId]

            // 1. 친구 관계 확인: 정보가 없거나 'accepted'가 아니면 "알 수 없음"
            val isFriend = info != null && info.friendshipStatus == "accepted"
            val displayImageUrl = if (isFriend) info?.profileImageUrl else null
            val displayName = if (isFriend) info!!.name else "알 수 없음"
            val displayIsOnline = if (isFriend) (info?.isOnline ?: false) else false

            if (info != null) {
                item.copy(
                    senderName = displayName,
                    profileImageUrl = displayImageUrl,
                    isOnline = displayIsOnline,
                    friendshipStatus = info.friendshipStatus
                )
            } else {
                // 정보가 아예 없는 경우에도 "알 수 없음" 처리
                item.copy(senderName = "알 수 없음")
            }
        }

        // 검색 필터링을 다시 적용해서 uiState 반영
        filterChatList(_searchQuery.value)
    }

    fun observeChatRooms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            chatRepository.observeChatRooms(myUid).collect { chatRooms ->
                try {
                    // 대화가 시작된 방
                    val activeChatRooms = chatRooms.filter { !it.lastMsg.isNullOrBlank() } // last_message가 없을 경우 필터링
                    // 내림차순
                    val sortedRooms = activeChatRooms.sortedByDescending { it.updatedAt }
                    // 병렬로 개별 채팅방 상세 정보 처리
                    val chatItems = sortedRooms.map { room ->
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

        // 1. 이미 정보가 있다면 즉시 반환 (캐시 우선)
        val cachedInfo = userInfos[receiverId]

        // 2. 정보가 없다면 최소한 이름/이미지는 확실히 가져오기 (리스너 등록과 별개로)
        if (cachedInfo == null && receiverId.isNotBlank()) {
            try {
                val userDoc = db.collection("Users").document(receiverId).get().await()
                if (userDoc.exists()) {
                    val name = userDoc.getString("nickname") ?: "이름 없음"
                    val imageUrl = userDoc.getString("profile_image_url")
                    val isOnline = userDoc.getBoolean("is_online") ?: false
                    val friendshipStatus = userDoc.getString("friendship_status") ?: "none"

                    // 메모리에 저장
                    userInfos[receiverId] = UserInfo(name, imageUrl, isOnline, friendshipStatus)

                }
            } catch (e: Exception) { }
        }

        val info = userInfos[receiverId]

        val isFriend = info != null && info.friendshipStatus == "accepted"
        val displayName = if (isFriend) (info.name ?: "이름 없음") else "알 수 없음"
        val displayImageUrl = if (isFriend) info.profileImageUrl else null
        val displayIsOnline = if (isFriend) (info?.isOnline ?: false) else false

        android.util.Log.d("ChatListViewModel",
            "채팅방 상세 처리 -> 상대 UID: $receiverId, 이름: ${info?.name ?: "불명"}, 관계: ${info?.friendshipStatus ?: "정보없음"}"
        )

        // unreadCounts 조회는 DMs 관련이므로 유지
        var myUnreadCount = 0
        try {
            val dmDoc = db.collection("DMs").document(roomId).get().await()
            myUnreadCount = (dmDoc.get("unread_counts") as? Map<String, Number>)?.get(myUid)?.toInt() ?: 0
        } catch (e: Exception) { }

        return ChatItem(
            id = roomId,
            receiverId = receiverId,
            senderName = displayName,
            profileImageUrl = displayImageUrl,
            lastMessage = room.lastMsg,
            time = formatChatTime(room.updatedAt),
            isRead = myUnreadCount <= 0,
            unreadCount = myUnreadCount,
            isOnline = displayIsOnline,
            friendshipStatus = info?.friendshipStatus ?: "none",
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