package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.mapper.toDomain
import com.bbip.bbipit.data.source.remote.user.UserRemoteDataSource
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 관련 데이터 처리를 담당하는 구현체입니다.
 * 프로필 관리 및 친구 관계 설정을 관리합니다.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource
) : UserRepository {

    //기존 fcm 토큰 획득
    override suspend fun getFcmToken(): String? = userRemoteDataSource.getToken()

    // 프로필 정보 업데이트
    override suspend fun updateProfile(nickname: String?, status: String?, profileImageUrl: String?, fcmToken: String?): Result<String> {
        return try {
            val message = userRemoteDataSource.updateProfile(nickname, status, profileImageUrl, fcmToken)
            Result.Success(message)
        } catch (e: Exception) {
            Log.e("UserRepository", "프로필 업데이트 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알 수 없는 오류"))
        }
    }

    // 친구 요청 전송
    override suspend fun sendFriendRequest(targetUid: String): Result<String> {
        return try {
            val message = userRemoteDataSource.sendFriendRequest(targetUid)
            Result.Success(message)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 요청 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 요청 중 오류 발생"))
        }
    }

    // 친구 삭제
    override suspend fun deleteFriend(targetUid: String): Result<String> {
        return try {
            val message = userRemoteDataSource.deleteFriend(targetUid)
            Result.Success(message)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 삭제 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 삭제 중 오류 발생"))
        }
    }

    // 수락된 친구 목록 조회
    override suspend fun getMyAcceptedFriends(): Result<List<User>> {
        return try {
            val friends = userRemoteDataSource.getMyAcceptedFriends()
            Result.Success(friends)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 목록 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 목록을 가져오지 못했습니다."))
        }
    }

    // 사용자 생명주기 업데이트
    override suspend fun updateHeartbeat(currentRoomId: String?): Result<Unit> {
        return try {
            userRemoteDataSource.updateHeartbeat(currentRoomId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Heartbeat 업데이트 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "Heartbeat 실패"))
        }
    }

    // 온라인 상태 업데이트
    override suspend fun updateOnlineStatus(isOnline: Boolean): Result<Boolean> {
        return try {
            val isSuccess = userRemoteDataSource.updateOnlineStatus(isOnline)
            Result.Success(isSuccess)
        } catch (e: Exception) {
            Log.e("UserRepository", "온라인 상태 업데이트 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "상태 업데이트 실패"))
        }
    }

    // 친구 요청 수락
    override suspend fun acceptFriendRequest(targetUid: String): Result<Boolean> {
        return try {
            val isSuccess = userRemoteDataSource.acceptFriendRequest(targetUid)
            Result.Success(isSuccess)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 수락 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 수락 실패"))
        }
    }

    // 친구 요청 거절
    override suspend fun declineFriendRequest(targetUid: String): Result<Boolean> {
        return try {
            val isSuccess = userRemoteDataSource.declineFriendRequest(targetUid)
            Result.Success(isSuccess)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 거절 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 거절 실패"))
        }
    }

    // 유저 프로필 조회
    override suspend fun getUserProfile(targetUid: String): Result<User> {
        return try {
            val response = userRemoteDataSource.getUserProfile(targetUid)
            val success = response?.get("success") as? Boolean ?: false
            val profileMap = response?.get("profile") as? Map<String, Any>

            if (success && profileMap != null) {
                Result.Success(profileMap.toDomain())
            } else {
                Result.Failure(AppError.Unknown("유저 정보를 찾을 수 없습니다."))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "유저 프로필 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "프로필 조회 실패"))
        }
    }

    // 내 프로필 조회 구현
    override suspend fun getMyProfile(uid: String): Result<User> {
        return try {
            val response = userRemoteDataSource.getMyProfile(uid)
            val success = response?.get("success") as? Boolean ?: false
            val profileMap = response?.get("profile") as? Map<String, Any>

            if (success && profileMap != null) {
                Result.Success(profileMap.toDomain())
            } else {
                Result.Failure(AppError.Unknown("내 정보를 찾을 수 없습니다."))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "내 프로필 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "내 프로필 조회 실패"))
        }
    }

    // 친구 프로필 및 상태 조회
    override suspend fun getFriendProfileWithStatus(targetUid: String): Result<Pair<User, String>> {
        return try {
            val response = userRemoteDataSource.getFriendProfileWithStatus(targetUid)
            val profileMap = response["profile"] as? Map<String, Any> ?: throw Exception("친구 정보를 찾을 수 없습니다.")
            val user = profileMap.toDomain()
            val friendshipStatus = response["friendship_status"] as? String ?: "none"
            Result.Success(Pair(user, friendshipStatus))
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 프로필 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 상태 조회 실패"))
        }
    }
}