package com.bbip.bbipit.data.repository

import android.net.Uri
import android.util.Log
import androidx.core.util.remove
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.mapper.toDomain
import com.bbip.bbipit.data.source.model.FriendshipDto
import com.bbip.bbipit.data.source.remote.user.UserRemoteDataSource
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

/**
 * 유저 프로필 및 상태 관리
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val liveStatusRepositoryProvider: Lazy<LiveStatusRepository>,
) : UserRepository {

    /**
     * 유저 고유 코드를 이용한 유저 검색
     */
    override suspend fun getUserProfileByCode(targetCode: String): Result<User> {
        return try {
            val response = userRemoteDataSource.getUserProfileByCode(targetCode)
            val success = response?.get("success") as? Boolean ?: false
            val profileMap = response?.get("profile") as? Map<String, Any>

            if (success && profileMap != null) {
                val userEntity = profileMap.toDomain()
                Result.Success(userEntity)
            } else {
                Result.Failure(AppError.Unknown("해당 코드를 사용하는 사용자를 찾을 수 없습니다."))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "코드로 유저 검색 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "유저 검색 중 오류가 발생했습니다."))
        }
    }

    /**
     * 특정 유저의 온라인 접속 상태 직접 조회 함수
     */
    override suspend fun getUserOnlineStatus(uid: String): Result<Boolean> {
        return try {
            val isOnline = userRemoteDataSource.getUserOnlineStatus(uid)
            if (isOnline != null) {
                Result.Success(isOnline)
            } else {
                Result.Failure(AppError.Unknown("해당 유저의 온라인 상태 정보를 가져올 수 없습니다."))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "온라인 상태 직접 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "온라인 상태 조회 실패"))
        }
    }

    /**
     * 알림 푸시 토큰 조회 함수
     */
    override suspend fun getFcmToken(): String? = userRemoteDataSource.getToken()



    /**
     * 유저 프로필 정보 및 푸시 토큰 업데이트 함수
     */
    override suspend fun updateProfile(
        nickname: String?,
        status: String?,
        profileImageUrl: Uri?,
        fcmToken: String?,
        sessionId: String?
    ): Result<String> {
        return try {
            val uploadImage = profileImageUrl?.let {
                userRemoteDataSource.uploadProfileImage(it)
            }

            // 원격 서버의 프로필 데이터 수정
            val message =
                userRemoteDataSource.updateProfile(nickname, status, uploadImage, fcmToken, sessionId)
            Result.Success(message)
        } catch (e: FirebaseFunctionsException) {
            // 업데이트 실패 예외 처리
            Log.e("UserRepository", "프로필 업데이트 실패: ${e.message}")
            e.printStackTrace()
            when(e.code){
                FirebaseFunctionsException.Code.UNAVAILABLE,
                FirebaseFunctionsException.Code.INTERNAL -> Result.Failure(AppError.Network())

                else -> Result.Failure(AppError.Server())
            }
        } catch (e: Exception){
            Log.e("유저 레퍼지토리", "프로필 업데이트 실패 ${e.message}")
            e.printStackTrace()
            if (e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException)
                Result.Failure(AppError.Network())
            else
                Result.Failure(AppError.Unknown())
        }
    }

    /**
     * 온라인 접속 상태 업데이트 함수
     */
//    override suspend fun updateOnlineStatus(isOnline: Boolean): Result<Boolean> {
//        return try {
//            // 원격 서버에 온라인 상태 저장
//            val isSuccess = userRemoteDataSource.updateOnlineStatus(isOnline)
//            Result.Success(isSuccess)
//        } catch (e: Exception) {
//            // 상태 변경 실패 예외 처리
//            Log.e("UserRepository", "온라인 상태 업데이트 실패: ${e.message}")
//            Result.Failure(AppError.Unknown(e.message ?: "상태 업데이트 실패"))
//        }
//    }

    /**
     * 다른 유저의 프로필 정보 조회 함수
     */
    override suspend fun getUserProfile(targetUid: String): Result<User> {
        return try {
            // 원격 데이터 조회 및 유효성 검증
            val response = userRemoteDataSource.getUserProfile(targetUid)
            val success = response?.get("success") as? Boolean ?: false
            val profileMap = response?.get("profile") as? Map<String, Any>

            // 검증 성공 시 도메인 엔티티로 변환하여 반환
            if (success && profileMap != null) {
                Result.Success(profileMap.toDomain())
            } else {
                Result.Failure(AppError.Unknown("유저 정보를 찾을 수 없습니다."))
            }
        } catch (e: Exception) {
            // 조회 실패 예외 처리
            Log.e("UserRepository", "유저 프로필 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "프로필 조회 실패"))
        }
    }

    /**
     * 내 프로필 상세 정보 조회 함수
     */
    override suspend fun getMyProfile(uid: String): Result<User> {
        return try {
            // 원격 저장소에서 내 프로필 조회 및 유효성 검증
            val response = userRemoteDataSource.getMyProfile(uid)
            val success = response?.get("success") as? Boolean ?: false
            val profileMap = response?.get("profile") as? Map<String, Any>

            Log.d("UserRepository", "디비 데이터: $response")
            Log.d("UserRepository", "profileMap: $profileMap")
            // 검증 성공 시 도메인 엔티티로 변환하여 반환
            if (response != null) {
                Result.Success(response.toDomain())
            } else {
                Result.Failure(AppError.Unknown("내 정보를 찾을 수 없습니다."))
            }
        } catch (e: Exception) {
            // 조회 실패 예외 처리
            Log.e("UserRepository", "내 프로필 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "내 프로필 조회 실패"))
        }
    }
}