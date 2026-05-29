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
    private val credentialManager: CredentialManager
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

//    fun signInWithSocial(type : LoginType){
//        updateState { copy(isLoading = true) }
//        viewModelScope.launch {
//
//            val result = when(type){
//                LoginType.KAKAO -> authRepository.signInWithKakao()
////                LoginType.GOOGLE -> authRepository.signInWithGoogle(context)
//                else -> return@launch
//            }
//            result.onSuccess {
//                updateState { copy(isLoading = false) }
//            }
//                .onFailure { exception ->
//                    updateState { copy(isLoading = false, error = exception.message) }
//
//                }
//        }

//    }

//    fun signInWithSocial(idToken: String, type: LoginType) {
//        updateState { copy(isLoading = true) }
//
//        viewModelScope.launch {
//            authRepository.signInWithCustomToken(idToken, type)
//            .onSuccess {
//                getFcmToken()
//                updateState { copy(isLoading = false) }
//
//            }.onFailure { exception ->
//                updateState { copy(isLoading = false, error = exception.message) }
//
//            }
//        }
//    }
    fun signInWithSocial(context: Context, type: LoginType) {
        updateState { copy(isLoading = true) }

        viewModelScope.launch {
            runCatching {
                when (type) {
                    LoginType.GOOGLE -> loginWithGoogle(context)
                    LoginType.KAKAO -> loginWithKaKao(context)
                    else -> null
                }
            }.onSuccess { idToken ->
                // 토큰을 정상적으로 받아왔을 때만 레포지토리 호출
                if (idToken != null) {
                    authRepository.signInWithCustomToken(idToken, type)
                        .onSuccess {
                            _eventChannel.send(SignInEvent.NavigateToHome)
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

    private suspend fun loginWithGoogle(context: Context): String? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false) // 구글 로그인 시도 시 핸드폰에 연결된 모든 계정 다이얼로그로 표출
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try{
            val result = credentialManager.getCredential(context, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            googleIdTokenCredential.idToken
        } catch (e: GetCredentialException) {

            if (e.javaClass.simpleName.contains("Canceled") || e.message?.contains("cancel", ignoreCase = true) == true) {
                throw AppError.Auth("구글 로그인 취소")
            } else {
                Log.e("GoogleLogin", "자격 증명 로드 실패: ${e.message}")
                e.printStackTrace()
                throw AppError.Auth("구글 계정을 불러올 수 없습니다.")
            }
        } catch (e: Exception){
            Log.e("구글 로그인 오류 발생", e.message.toString())
            e.printStackTrace()
            throw AppError.Auth("오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
        }
    }

    private suspend fun loginWithKaKao(context: Context): String= suspendCancellableCoroutine { continuation ->
        val userClient = UserApiClient.instance

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if(error != null){
                //카카오 웹 로그인 취소용 에러 메세지
                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                    continuation.resumeWithException(AppError.Auth("카카오 로그인 취소"))
                }else{
                    Log.e("카카오 클라이언트 오류", error.message.toString())
                    error.printStackTrace()
                    continuation.resumeWithException(AppError.Unknown("오류가 발생했습니다. 잠시 후 다시 시도해주세요."))
                }

            } else if (token != null){
                val idToken = token.idToken
                if (idToken != null){
                    continuation.resume(idToken)
                }else{
                    Log.e("카카오 소셜 로그인 오류", "id 토큰 발급 실패, OpenId Connect 확인 필요 : $error")
                    continuation.resumeWithException(AppError.Unknown("오류가 발생했습니다. 잠시 후 다시 시도해주세요."))
                }
            }
        }
        if (userClient.isKakaoTalkLoginAvailable(context)){
            userClient.loginWithKakaoTalk(context) { token, error ->
                if(error != null){
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled){
                        continuation.resumeWithException(AppError.Auth("카카오 로그인 취소"))
                        return@loginWithKakaoTalk
                    }
                    userClient.loginWithKakaoAccount(context, callback = callback)
                } else if ( token != null){
                    val idToken = token.idToken
                    if (idToken != null) continuation.resume(idToken)
                    else userClient.loginWithKakaoAccount(context, callback = callback)
                }
            }
        } else{
            userClient.loginWithKakaoAccount(context, callback = callback)
        }
    }
}