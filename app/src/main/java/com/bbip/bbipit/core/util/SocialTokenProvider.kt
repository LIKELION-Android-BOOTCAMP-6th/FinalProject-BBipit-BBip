package com.bbip.bbipit.core.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.bbip.bbipit.R
import com.bbip.bbipit.domain.error.AppError
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class SocialTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialManager: CredentialManager) {
    suspend fun fromGoogle(context: Context): String? {

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(true)
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

    suspend fun fromKakao(context: Context): String= suspendCancellableCoroutine { continuation ->
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

    suspend fun revokeGoogle(){
        val authorizationClient = Identity.getAuthorizationClient(context)
        val revokeRequest = RevokeAccessRequest.builder().build()
        authorizationClient.revokeAccess(revokeRequest).await()
    }
}