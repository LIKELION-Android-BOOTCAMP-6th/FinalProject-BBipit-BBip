package com.bbip.bbipit.data.repository

import android.content.Context
import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.type.LoginType
import com.bbip.bbipit.presentation.auth.ui.TermsType
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.functions.FirebaseFunctionsException
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

    override fun isEmailVerified(): Boolean {
        return authRemoteDataSource.isEmailVerified()
    }

    override fun isAutoLogin(): Boolean = authRemoteDataSource.isAutoLogin()
    // 카카오 로그인 수행
    override suspend fun signInWithKakao(): Result<Unit> {
        return try {
            val accessToken = authRemoteDataSource.loginWithKakao()
            authRemoteDataSource.signInWithCustomToken(accessToken, LoginType.KAKAO)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("Auth", "카카오 로그인 실패: ${e.message}")
            val appError = when (e) {
                is ClientError -> {
                    if (e.reason == ClientErrorCause.Cancelled) {
                        AppError.Auth("카카오 로그인 취소")
                    } else {
                        AppError.Unknown(e.message ?: "카카오 클라이언트 오류")
                    }
                }
                else -> AppError.Unknown(e.message ?: "로그인 중 오류 발생")
            }
            Result.Failure(appError)
        }
    }

    // 구글 ID 토큰을 이용한 로그인 수행
    override suspend fun signInWithGoogle(appContext: Context): Result<Unit> {
        return try {
            val accessToken = authRemoteDataSource.loginWithGoogle(appContext) ?: throw Exception(AppError.Auth("구글 계정 불러오기를 실패했습니다. 다시 시도해주세요."))
            authRemoteDataSource.signInWithCustomToken(accessToken, LoginType.GOOGLE)
            Result.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Auth", "구글 로그인 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "구글 로그인 중 오류 발생"))
        }
    }

    // 이메일 기반 회원가입 수행
    override suspend fun signUpWithEmail(email: String, password: String, nickname: String): Result<AuthResult> {
        return try {
            val authResult = authRemoteDataSource.signUpWithEmail(email, password, nickname)
//            val user = authResult.user
//            user?.let {
//                it.sendEmailVerification().await()
//            }
//            Log.d("Auth", "회원가입 성공: ${user?.email} (UID: ${user?.uid})")

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
                "ERROR_INVALID_EMAIL" -> AppError.Email("올바른 이메일 형식이 아닙니다.")
                "ERROR_WEAK_PASSWORD", "PASSWORD_DOES_NOT_MEET_REQUIREMENTS" -> AppError.Password()
                else -> AppError.Auth(e.errorCode)
            }
            Log.e("Auth", "회원가입 실패: ${e.errorCode}")
            Result.Failure(error)

        } catch (e: Exception){
            Log.e("Auth", "회원가입 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "회원가입 중 오류 발생"))

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
            val error = when(e.errorCode){
                "ERROR_INVALID_EMAIL" -> AppError.Email()
                "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL"
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