package com.bbip.bbipit.presentation.mypage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewmodel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // UI 상태를 관리하는 StateFlow
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    /**
     * 1. 텍스트 복사 로직 (클립보드 연동)
     */
    // 토스트 메시지를 전달할 단발성 이벤트 단자
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BBIP_ID", text)
        clipboard.setPrimaryClip(clip)

        viewModelScope.launch {
            _toastEvent.emit("ID가 클립보드에 복사되었습니다.")
        }
    }

    /**
     * 2. 프로필 변경 사항 임시 수신 (화면 동기화용)
     * 나중에 EditProfileScreen에서 백스택에 데이터를 실어 보내거나,
     * 공유 ViewModel, 혹은 DB 변경 리스너를 달 때 이 함수를 호출해 UI를 갱신합니다.
     */
    fun updateProfile(newNickname: String, newStatus: String) {
        _uiState.update {
            it.copy(nickname = newNickname, status = newStatus)
        }
    }

    /**
     * 3. 카카오톡 공유 로직 (틀 미리 잡기)
     */
    fun shareToKakao(id: String) {
        // TODO: 카카오 SDK 메시지 공유 API 호출부
        Toast.makeText(context, "카카오톡으로 ID를 공유합니다.", Toast.LENGTH_SHORT).show()
    }

    /**
     * Firestore에서 내 프로필 정보를 실시간으로 가져오는 함수
     */
    fun fetchUserProfile(uid: String) {
        _uiState.update { it.copy(isLoading = true) } // 로딩 시작

        firestore.collection("Users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "데이터를 불러올 수 없습니다.") }
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val nickname = snapshot.getString("nickname") ?: "익명"
                    val status = snapshot.getString("status") ?: ""
                    val profileImageUrl = snapshot.getString("profile_image_url") ?: ""

                    _uiState.update {
                        it.copy(
                            nickname = nickname,
                            status = status,
                            profileImageUrl = profileImageUrl,
                            isLoading = false // 로딩 완료
                        )
                    }
                }
            }
    }
}