package com.bbip.bbipit.presentation.auth.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.core.util.SocialTokenProvider
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.type.LoginType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.edit

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val pwError: String? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isDuplicatedInfoDialog : Boolean = false,
    val loginType: LoginType = LoginType.EMAIL,
)
sealed class SignInEvent{
    object NavigateToHome: SignInEvent()
    object NavigateToSignUp: SignInEvent()
}
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val socialTokenProvider: SocialTokenProvider
) : BaseViewModel<SignInUiState>(SignInUiState()) {

    private val _eventChannel = Channel<SignInEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()
    private var _serverToken: String? = null //DB에 저장되어 있던 FCM 토큰
    private var _token: String? = null //파이어베이스에서 가져오는 FCM 토큰
    fun onUpdateEmail(email : String) = updateState{ copy(email =  email, emailError = null)}
    fun onUpdatePassword(pw: String) = updateState { copy(password = pw, pwError = null) }

    private suspend fun getServerToken(): String?{
        val uid = authRepository.getCurrentUserUid()
        var token : String? = null
        uid?.let {
            userRepository.getMyProfile(uid)
                .onSuccess { data ->
                    token = data.fcmToken
                }
                .onFailure {
                    token = null
                }
        }
        return token
    }
    fun signIn(){
        updateState { copy(isLoading = true, emailError = "", pwError = "", loginType = LoginType.EMAIL) }
        viewModelScope.launch {
            authRepository.signInWithEmail(uiState.value.email, uiState.value.password)
                .onSuccess {
                    _serverToken = getServerToken()

                    Log.d("로그인 시도" , "서버 토큰 : $_serverToken, 중복 여부 : ${checkDuplicateLogin()}")
                    if (!_serverToken.isNullOrBlank() && checkDuplicateLogin()) {
                        updateState { copy(isDuplicatedInfoDialog = true, isLoading = false) }
                    } else {
                        continueLogin(true)
                    }

                }
                .onFailure { exception ->
                    updateState { copy(isLoading = false) }
                    when(exception){
                        is AppError.Email -> updateState { copy(email = "", emailError = exception.message) }
                        is AppError.Password ->
                            updateState { copy( email= "", password = "", emailError = exception.message, pwError = exception.message) }
                        else -> updateState { copy(error = exception.message) }
                    }
                }

        }
    }
    private suspend fun checkDuplicateLogin(): Boolean {
        _token = userRepository.getFcmToken()
        _token?.let {
            return _serverToken != it
        }
        return false //서버 오류로 토큰 값이 널일 경우 (거의 없음)
    }

    fun continueLogin(isContinue: Boolean){
        //로그인
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            if (isContinue){
                userRepository.updateProfile(fcmToken = _token )
                updateState { copy(isLoading = false) }
                _eventChannel.send(SignInEvent.NavigateToHome)
            }else{
                updateState { copy(isLoading = false, isDuplicatedInfoDialog = false, error = "로그인 취소", email = "", password = "") }
                authRepository.signOut(uiState.value.loginType)

            }
        }
    }

    fun moveToSignUp(){
        viewModelScope.launch {
            _eventChannel.send(SignInEvent.NavigateToSignUp)
        }
    }

    fun signInWithSocial(context: Context, type: LoginType) {
        updateState { copy(isLoading = true) }

        viewModelScope.launch {
            runCatching {
                when (type) {
                    LoginType.GOOGLE -> {
                        updateState { copy(loginType = LoginType.GOOGLE) }
                        socialTokenProvider.fromGoogle(context)
                    }
                    LoginType.KAKAO -> {
                        updateState { copy(loginType = LoginType.KAKAO) }
                        socialTokenProvider.fromKakao(context)
                    }
                    else -> null
                }
            }.onSuccess { idToken ->
                // 토큰을 정상적으로 받아왔을 때만 레포지토리 호출
                if (idToken != null) {
                    authRepository.signInWithCustomToken(idToken, type)
                        .onSuccess {
                            _serverToken = getServerToken()

                            Log.d("로그인 시도" , "서버 토큰 : $_serverToken, 중복 여부 : ${checkDuplicateLogin()}")
                            if (!_serverToken.isNullOrBlank() && checkDuplicateLogin()) {
                                updateState { copy(isDuplicatedInfoDialog = true, isLoading = false) }
                            } else {
                                continueLogin(true)
                            }

                        }
                        .onFailure {exception ->
                            Log.e("SocialLogin", "소셜 연동 실패: ${exception.message}")
                            updateState { copy(isLoading = false, error = exception.message) }
                        }
                }
            }.onFailure { exception ->
                Log.e("SocialLogin", "로그인 실패: ${exception.message}")
                updateState { copy(isLoading = false, error = exception.message) }
            }
        }
    }

    fun onUpdateToast(value:String? = null) = updateState { copy(error = value) }
    fun onUpdateDuplicatedInfoDialog(value: Boolean) = updateState { copy(isDuplicatedInfoDialog = value) }
}