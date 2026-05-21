package com.bbip.bbipit.data.source.remote.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.bbip.bbipit.R
import com.bbip.bbipit.domain.type.LoginType
import com.bbip.bbipit.presentation.auth.ui.TermsType
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.oAuthCredential
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 인증 관련 원격 데이터 소스 구현체입니다.
 * Firebase Auth를 사용하여 사용자 인증 처리를 수행합니다.
 */
@Singleton
class AuthRemoteDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager
) : AuthRemoteDataSource {
    override fun isAutoLogin(): Boolean = firebaseAuth.currentUser != null
    // 카카오 로그인
    override suspend fun loginWithKakao(): String = suspendCancellableCoroutine { continuation ->
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if(error != null){
                continuation.resumeWithException(error)
            } else if (token != null){
                val idToken = token.idToken
                if (idToken != null){
                    continuation.resume(idToken)
                }else{
                    continuation.resumeWithException(IllegalStateException("카카오 로그인 OpenID Connect 설정 확인 필요"))
                }
            }
        }
        val userClient = UserApiClient.instance
        if (userClient.isKakaoTalkLoginAvailable(context)){
            userClient.loginWithKakaoTalk(context) { token, error ->
                if(error != null){
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled){
                        continuation.resumeWithException(error)
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
    // 구글 로그인
    override suspend fun loginWithGoogle(appContext: Context): String? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false) // 구글 로그인 시도 시 핸드폰에 연결된 모든 계정 다이얼로그로 표출
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try{
            val result = credentialManager.getCredential(appContext, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            googleIdTokenCredential.idToken
        } catch (e: GetCredentialException) {
            // 🚨 노트10이나 테스터 폰에서 'failed to launch...'가 터지면 일로 들어옵니다!
            Log.e("GoogleLogin", "자격 증명 로드 실패: ${e.message}")
            throw e
            null
            // 여기서 무한 블로킹 안 걸리게 예외를 가공해서 뷰모델로 던져줍니다.
        } catch (e: Exception){
            throw e
            null
        }
    }

    // 커스텀 토큰 로그인
    override suspend fun signInWithCustomToken(accessToken: String, type: LoginType) {
        when(type){
            LoginType.KAKAO -> {
                val providerId = "oidc.kakao"
                val credential = oAuthCredential(providerId) { setIdToken(accessToken) }
                firebaseAuth.signInWithCredential(credential).await()
            }

            LoginType.GOOGLE -> {
                Log.d("데이터리모트", "구글 로그인 in 커스텀 토큰")
                val credential = GoogleAuthProvider.getCredential(accessToken, null)
                firebaseAuth.signInWithCredential(credential).await()
            }

            else -> {}
        }
    }

    // 이메일 회원가입
    override suspend fun signUpWithEmail(email: String, password: String, nickname: String): AuthResult {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()

        val user = authResult.user

        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(nickname)
            .build()

        user?.updateProfile(profileUpdate)?.await()

        return authResult
    }

    // 이메일 로그인
    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    // 현재 사용자 ID 반환
    override fun getCurrentUserUid(): String? {
        return firebaseAuth.currentUser?.uid
    }

    // 인증 상태 흐름 관찰
    override fun getAuthStateFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        firebaseAuth.addAuthStateListener(listener)

        // 리스너 해제
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun getTerms(type: TermsType): String {
        val rawUrl = if (type == TermsType.PRIVACY) {
            "https://raw.githubusercontent.com/LIKELION-Android-BOOTCAMP-6th/FinalProject-BBipit-BBip/refs/heads/develop/docs/terms.md"
        } else {
            "https://raw.githubusercontent.com/LIKELION-Android-BOOTCAMP-6th/FinalProject-BBipit-BBip/refs/heads/develop/docs/service.md"
        }
        return withContext(Dispatchers.IO) {
            URL(rawUrl).readText()
        }
    }
}