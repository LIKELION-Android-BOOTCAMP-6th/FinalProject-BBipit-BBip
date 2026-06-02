package com.bbip.bbipit.presentation.mypage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.type.LoginType
import com.bbip.bbipit.domain.type.TermsType
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyPageUiState(
    val nickname: String = "불러오는 중...",
    val status: String = "",
    val profileImageUrl: String = "",
    val uniqueId: String = "",
    val isLoading: Boolean = true, // 로딩 중
    val errorMessage: String? = null, // 에러
    val isSignOutDialogShown: Boolean = false,
    val email: String = "",
    val loginType: String  = "",
    val toast: String? = null,
    val userCode: String = ""
)

sealed class MyPageEvent{
    object NavigateToSignIn: MyPageEvent()

}
@HiltViewModel
class MyPageViewmodel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // UI 상태를 관리하는 StateFlow
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private val _terms = MutableStateFlow("")
    val terms: StateFlow<String> = _terms.asStateFlow()

    private val _event = Channel<MyPageEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()
    /**
     * 1. 텍스트 복사 로직 (클립보드 연동)
     */
    // 토스트 메시지를 전달할 단발성 이벤트 단자

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BBIP_ID", text)
        clipboard.setPrimaryClip(clip)

        onUpdateToast("내 CODE가 복사되었습니다. 해당 코드로 친구를 추가해보세요!")
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

        onUpdateToast("카카오톡으로 ID를 공유합니다.")
    }

    /**
     * Firestore에서 내 프로필 정보를 실시간으로 가져오는 함수
     */
    fun fetchUserProfile() {
        _uiState.update { it.copy(isLoading = true) } // 로딩 시작
        val uid = authRepository.getCurrentUserUid()

        Log.d("유아이디", uid.toString())
        if (uid == null){
            _uiState.update { it.copy(isLoading = false, toast = "정보를 불러올 수 없습니다.") }
        }
        uid?.let {
            viewModelScope.launch {
                //직접 디비와 통신하는 게 아닌 유저 레포지토리에 선언되어 있는 함수를 통해서 정보 가져오기
                userRepository.getMyProfile(it)
                    .onSuccess {  user ->
                        Log.d("프로필 받아오기 성공", user.toString())
                        _uiState.update { state ->
                            state.copy(
                                nickname = user.nickname,
                                status = user.status,
                                profileImageUrl = user.profileImageUrl,
                                email = user.email,
                                loginType = user.loginType,
                                isLoading = false,
                                userCode = user.userCode
                            )
                        }
                    }
                    .onFailure { exception ->
                        Log.e("프로필 받아오기 실패", exception.message.toString())
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                toast = exception.message
                            )
                        }
                    }
            }
        }
    }

    fun onChangeSignOutDialog(value: Boolean) = _uiState.update { it.copy(isSignOutDialogShown = value) }
    fun signOut(){
        _uiState.update { it.copy(isLoading = true) }

        //스트링 -> LoginType으로 변경
        val loginType = LoginType.fromString(_uiState.value.loginType)
        viewModelScope.launch {
            authRepository.signOut(loginType)
            _uiState.update { it.copy(isLoading = false) }
            _event.send(MyPageEvent.NavigateToSignIn)
        }
    }

    fun onUpdateLoading(value: Boolean) = _uiState.update { it.copy(isLoading = value) }
    fun onUpdateToast(value: String?) = _uiState.update { it.copy(toast = value) }

    fun getTerms(currentType : TermsType){
        viewModelScope.launch {
            onUpdateLoading(true)
            authRepository.getTerms(currentType)
                .onSuccess {
                    onUpdateLoading(false)
                    _terms.value = it
                    Log.d("약관 내용", it)

                }
                .onFailure {
                    onUpdateLoading(false)
                    _terms.value = "<h2>오류</h2><p>약관을 불러오지 못했습니다. 네트워크를 확인해 주세요.</p>"
                }

        }

    }
}