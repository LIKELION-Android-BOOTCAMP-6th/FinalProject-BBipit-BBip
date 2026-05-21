package com.bbip.bbipit.presentation.auth.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.type.LoginType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
) : BaseViewModel<SignInUiState>(SignInUiState()) {

    private val _eventChannel = Channel<SignInEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    fun onUpdateEmail(email : String) = updateState{ copy(email =  email)}
    fun onUpdatePassword(pw: String) = updateState { copy(password = pw) }

    fun signIn(){
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            authRepository.signInWithEmail(uiState.value.email, uiState.value.password)
                .onSuccess {
                    updateState { copy(isLoading = false) }
                    getFcmToken()
                    _eventChannel.send(SignInEvent.NavigateToHome)
                }
                .onFailure { exception ->
                    updateState { copy(isLoading = false, email = "", password = "") }
                    when(exception){
                        is AppError.Email -> updateState { copy(emailError = exception.message) }
                        is AppError.Password ->
                            updateState { copy(emailError = exception.message, pwError = exception.message) }
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

    fun signInWithSocial(context: Context, type : LoginType){
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            val result = when(type){
                LoginType.KAKAO -> authRepository.signInWithKakao()
                LoginType.GOOGLE -> authRepository.signInWithGoogle(context)
                else -> return@launch
            }
            result.onSuccess {
                getFcmToken()
                updateState { copy(isLoading = false) }
                _eventChannel.send(SignInEvent.NavigateToHome)
            }
                .onFailure { exception ->
                    updateState { copy(isLoading = false, error = exception.message) }

                }
        }

    }
}