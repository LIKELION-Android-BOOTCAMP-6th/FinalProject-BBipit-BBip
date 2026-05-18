package com.bbip.bbipit.presentation.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.UserRepository
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
                    val token = userRepository.getFcmToken()
                    userRepository.updateProfile(fcmToken = token)
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
}