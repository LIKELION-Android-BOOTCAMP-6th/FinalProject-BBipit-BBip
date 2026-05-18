package com.bbip.bbipit.presentation.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.presentation.auth.ui.TermsType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URL
import javax.inject.Inject

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val checkPw: String = "",
    val terms: String = "",
    val isLoading: Boolean = false,
    val isNotiShown: Boolean = false,
    val emailError: String? = null,
    val pwError: String? = null,
    val error: String? = null

)

sealed class SignUpEvent{
    object NavigateToSignIn: SignUpEvent()
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
): ViewModel(){

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<SignUpEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()
    fun getTerms(currentType : TermsType){
         viewModelScope.launch {
             onUpdateLoading(true)
             authRepository.getTerms(currentType)
                 .onSuccess {
                     onUpdateLoading(false)
                     onUpdateTerms(it)
                 }
                 .onFailure {
                     onUpdateLoading(false)
                     onUpdateTerms("<h2>오류</h2><p>약관을 불러오지 못했습니다. 네트워크를 확인해 주세요.</p>")
                 }

        }
    }
    fun signUp(){
        viewModelScope.launch {
            onUpdateLoading(true)
            clearErrorMessage()
            if (!isValidEmail(_uiState.value.email)){
                onUpdateEmailError("이메일 형식이 일치하지 않습니다.")
                onUpdateLoading(false)
                clearAll()
                return@launch
            }

            if (!isValidPassword(_uiState.value.password)){
                onUpdatePwError("비밀번호 규칙이 올바르지 않습니다.")
                onUpdateLoading(false)
                clearAll()
                return@launch
            }

            authRepository.signUpWithEmail(_uiState.value.email, _uiState.value.password)
                .onSuccess {
                    onUpdateLoading(false)
                    userRepository.updateProfile(nickname = _uiState.value.name)
                    onUpdateNotiShown(true)
                }
                .onFailure { exception ->
                    onUpdateLoading(false)
                    _uiState.update { currentState ->
                        when(exception){
                            is AppError.Email -> {
                                currentState.copy(emailError = exception.message)
                            }
                            is AppError.Password -> {
                                currentState.copy(pwError = exception.message)
                            }
                            else -> {
                                currentState.copy(error = exception.message)
                            }
                        }
                    }
                    clearAll()

                }
        }
    }

    private fun onUpdateTerms(value: String) =  _uiState.update { it.copy(terms = value) }
    private fun onUpdateLoading(value: Boolean) = _uiState.update { it.copy(isLoading = value) }
    fun onUpdateName(name: String) = _uiState.update { it.copy(name = name) }
    fun onUpdateEmail(email: String) = _uiState.update { it.copy(email = email) }
    fun onUpdatePassword(password: String) = _uiState.update { it.copy(password = password) }
    fun onUpdateCheckPw(password: String) = _uiState.update { it.copy(checkPw = password) }
    fun onUpdateNotiShown(value: Boolean) = _uiState.update { it.copy(isNotiShown = value) }
    private fun onUpdatePwError(value: String) = _uiState.update { it.copy(pwError = value) }
    private fun onUpdateEmailError(value: String) = _uiState.update { it.copy(emailError = value) }
    private fun clearAll() = _uiState.update {
        it.copy(name = "", email = "", password = "", checkPw = "")
    }
    fun clearErrorMessage() = _uiState.update { it.copy(emailError = null, pwError = null) }
    fun moveToSignIn(){
        viewModelScope.launch {
            _event.send(SignUpEvent.NavigateToSignIn)
        }
    }
    private fun isValidPassword(password: String): Boolean {
        val regex = Regex("^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#\$%^&*()_+\\-=]).{8,}$")
        return regex.matches(password)
    }
    private fun isValidEmail(email: String): Boolean {
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return regex.matches(email)
    }

}