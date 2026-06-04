package com.bbip.bbipit.presentation.auth.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.R
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.core.util.SocialTokenProvider
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.type.LoginType
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val pwError: String? = null,
    val error: String? = null,
    val isLoading: Boolean = false,

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

    fun onUpdateEmail(email : String) = updateState{ copy(email =  email)}
    fun onUpdatePassword(pw: String) = updateState { copy(password = pw) }

    fun signIn(){
        updateState { copy(isLoading = true, emailError = "", pwError = "") }
        viewModelScope.launch {
            authRepository.signInWithEmail(uiState.value.email, uiState.value.password)
                .onSuccess {
                    updateState { copy(isLoading = false) }
                    getFcmToken()
                    _eventChannel.send(SignInEvent.NavigateToHome)
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

    fun moveToSignUp(){
        viewModelScope.launch {
            _eventChannel.send(SignInEvent.NavigateToSignUp)
        }
    }
    suspend fun getFcmToken(){
        val token = userRepository.getFcmToken()
        userRepository.updateProfile(fcmToken = token)
    }

    fun signInWithSocial(context: Context, type: LoginType) {
        updateState { copy(isLoading = true) }

        viewModelScope.launch {
            runCatching {
                when (type) {
                    LoginType.GOOGLE -> socialTokenProvider.fromGoogle(context)
                    LoginType.KAKAO -> socialTokenProvider.fromKakao(context)
                    else -> null
                }
            }.onSuccess { idToken ->
                // 토큰을 정상적으로 받아왔을 때만 레포지토리 호출
                if (idToken != null) {
                    authRepository.signInWithCustomToken(idToken, type)
                        .onSuccess {
                            _eventChannel.send(SignInEvent.NavigateToHome)
                            getFcmToken()
                            updateState { copy(isLoading = false) }
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
}