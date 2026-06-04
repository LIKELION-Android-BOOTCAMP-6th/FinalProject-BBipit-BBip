package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.type.LoginType
import com.bbip.bbipit.domain.type.TermsType
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.oAuthCredential
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 관련 데이터 처리를 담당하는 구현체입니다.
 * Firebase 및 외부 인증 서비스와의 상호작용을 관리합니다.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val firebaseAuth: FirebaseAuth
): AuthRepository {

    override suspend fun signOut(type: LoginType){
        when(type){
            LoginType.GOOGLE -> authRemoteDataSource.signOutGoogle()
            LoginType.KAKAO -> authRemoteDataSource.signOutKakao()
            else -> {}
        }

        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount(type: LoginType, token: String ): Result<Unit> {
        val user = firebaseAuth.currentUser!!
        val credential = when(type){
            LoginType.GOOGLE -> GoogleAuthProvider.getCredential(token, null)
            LoginType.KAKAO -> {
                val providerId = "oidc.kakao"
                oAuthCredential(providerId) { setIdToken(token) }
            }
            LoginType.EMAIL -> EmailAuthProvider.getCredential(user.email!!, token)
        }

        return  try {
            user.reauthenticate(credential).await()
            user.delete().await()
            Result.Success(Unit)
        } catch (e: Exception){
            Log.e("회원 탈퇴 실패", "탈퇴 실패 $type ${e.message}")
            e.printStackTrace()
            Result.Failure(AppError.Auth("탈퇴 실패"))
        }
    }

    override fun isEmailVerified(): Boolean {
        return authRemoteDataSource.isEmailVerified()
    }

    override fun isAutoLogin(): Boolean = authRemoteDataSource.isAutoLogin()

    override suspend fun signInWithCustomToken(idToken: String, type: LoginType): Result<Unit> {
        return try {
            authRemoteDataSource.signInWithCustomToken(idToken, type)
            Result.Success(Unit)
        } catch (e: Exception){

            Log.e("${type.type} error", e.message.toString())
            e.printStackTrace()
            Result.Failure(AppError.Unknown("알 수 없는 오류 발생"))
        }
    }

    // 이메일 기반 회원가입 수행
    override suspend fun signUpWithEmail(email: String, password: String, nickname: String): Result<AuthResult> {
        return try {
            val authResult = authRemoteDataSource.signUpWithEmail(email, password, nickname)

            // reload()가 완료되어 기기에 안착한 가장 최신의 세션 유저 객체를 직접 다시 꺼내옵니다.
            val freshUser = firebaseAuth.currentUser
            freshUser?.let { user ->
                Log.d("Auth", "🔥 [인증 세션 갱신 성공] 이메일 발송을 지시합니다. Target: ${user.email}")
                // 확실히 확보된 최신 토큰 세션 자격으로 메일 전송
                user.sendEmailVerification().await()
                Log.d("Auth", "🎉 Firebase 서버가 인증 메일 발송을 정상 접수했습니다!")
            } ?: throw Exception("인증 메일을 발송할 유저 세션을 획득하지 못했습니다.")

            // 💡 [이중 안전장치]: 앞서 논의한 대로 자동 로그인 상태(토큰)를 기기에서 즉시 파기하여
            // 팝업이 떠 있는 동안이나 앱 재접속 시 미인증 유저가 자동 로그인되는 틈새를 원천 차단합니다.
            signOut()
            Log.d("Auth", "🔒 미인증 유저 우회 방지를 위해 즉시 로그아웃(세션 파기) 처리 완료")

            Result.Success(authResult)
        } catch (e: FirebaseAuthException) {
            Log.d("AuthDebug", "진짜 에러코드 추출 결과: [${e.errorCode}]")

            val error = when(e.errorCode){
                "ERROR_EMAIL_ALREADY_IN_USE" -> AppError.Email("이미 가입된 이메일 주소입니다.")
                "ERROR_INVALID_EMAIL" -> AppError.Email()
                "ERROR_WEAK_PASSWORD", "PASSWORD_DOES_NOT_MEET_REQUIREMENTS" -> AppError.Password()
                else -> AppError.Auth(e.errorCode)
            }
            Log.e("Auth", "회원가입 실패: ${e.errorCode}")
            Result.Failure(error)

        } catch (e: Exception){
            Log.e("Auth", "회원가입 실패: ${e.message}")
            e.printStackTrace()
            Result.Failure(AppError.Unknown("오류가 발생했습니다. 잠시 후 다시 시도해주세요"))

        }
    }

    // 이메일 기반 로그인 수행
    override suspend fun signInWithEmail(email: String, password: String): Result<AuthResult> {
        return try {
// 💡 [순서 교정]: 로그인을 먼저 시도해야 토큰이 들어오고 reload가 가능합니다. 기존에는 reload가 로그인 앞에 배치되어 오작동했습니다.
            val authResult = authRemoteDataSource.signInWithEmail(email, password)

            // 로그인 직후 서버의 최신 인증 상태를 기기로 동기화
            authRemoteDataSource.reloadCurrentUser()

            //  로그인은 성공했으나 이메일 인증이 완료되지 않은 유저인 경우
            if (!authRemoteDataSource.isEmailVerified()) {
                firebaseAuth.signOut()
                // 로그인 화면에 보여줄 커스텀 에러를 반환
                return Result.Failure(AppError.Auth("이메일 인증이 완료되지 않았습니다. 메일함을 확인해주세요."))
            }

            Log.d("Auth", "로그인 성공: ${authResult.user?.uid}")
            Result.Success(authResult)
        } catch (e: FirebaseAuthException){
            Log.e("이메일 에러 메세지 확인", "비번 오류 ${e.errorCode}")
            val error = when(e.errorCode){
                "ERROR_INVALID_EMAIL" -> AppError.Email()
                 "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL"
                    -> AppError.Password("이메일 또는 비밀번호가 올바르지 않습니다.")
                else -> AppError.Auth()

            }
            Log.e("Auth", "로그인 실패 ${e.errorCode}")
            Result.Failure(error)
        }catch (e: Exception) {
            Log.e("Auth", "로그인 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "로그인 중 오류 발생"))
        }
    }

    // 현재 사용자 UID 반환
    override fun getCurrentUserUid(): String? {
        return authRemoteDataSource.getCurrentUserUid()
    }

    // 인증 상태 변경 흐름 반환
    override fun getAuthStateFlow(): kotlinx.coroutines.flow.Flow<String?> {
        return authRemoteDataSource.getAuthStateFlow()
    }

    override suspend fun getTerms(type: TermsType): Result<String> {
        return try {
            val result = authRemoteDataSource.getTerms(type)
            Result.Success(result)
        } catch (e: Exception){
            e.printStackTrace()
            Log.e("Auth", "약관 불러오기 ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "약관 내용 읽어오기 실패"))
        }
    }
}