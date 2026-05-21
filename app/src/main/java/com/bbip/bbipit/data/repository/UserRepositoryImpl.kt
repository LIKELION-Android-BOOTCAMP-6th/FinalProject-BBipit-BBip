package com.bbip.bbipit.data.repository

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

/**
 * 프로필 관리, 친구 관계 설정 관장 및 유저 데이터 소스 계층 연동 저장소 구현체 클래스
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val liveStatusRepositoryProvider: Lazy<LiveStatusRepository>,
) : UserRepository {

    // 서버 영역 적재 기존 유효 푸시 서비스 토큰 코드 인출 함수
    override suspend fun getFcmToken(): String? = userRemoteDataSource.getToken()

    /**
     * 변경 프로필 내용 및 수신 토큰 상태의 데이터 소스 컴포넌트 경유 원격 갱신 함수
     */
    override suspend fun updateProfile(
        nickname: String?,
        status: String?,
        profileImageUrl: String?,
        fcmToken: String?
    ): Result<String> {
        return try {
            val message =
                userRemoteDataSource.updateProfile(nickname, status, profileImageUrl, fcmToken)
            Result.Success(message)
        } catch (e: Exception) {
            Log.e("UserRepository", "프로필 업데이트 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알 수 없는 오류"))
        }
    }

    /**
     * 순환 의존성 오류 회피 목적의 프로바이더 패턴 적용 및 수명 주기 상태 관리자 내 현재 체류 채팅방 ID 동기화 함수
     */
    override suspend fun updateHeartbeat(currentRoomId: String?): Result<Unit> {
        return try {
            liveStatusRepositoryProvider.get().updateLifeCycle(currentRoomId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Heartbeat 업데이트 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "Heartbeat 실패"))
        }
    }

    /**
     * 네트워크 소켓 온라인 연결 상태 플래그 실시간 토글 보정 함수
     */
    override suspend fun updateOnlineStatus(isOnline: Boolean): Result<Boolean> {
        return try {
            val isSuccess = userRemoteDataSource.updateOnlineStatus(isOnline)
            Result.Success(isSuccess)
        } catch (e: Exception) {
            Log.e("UserRepository", "온라인 상태 업데이트 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "상태 업데이트 실패"))
        }
    }

    /**
     * 타인 식별자 코드 기반 서버 원격 공용 프로필 맵 데이터 추적 및 도메인 규격 매칭 디코딩 함수
     */
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

    /**
     * 내 고유 계정 키값 파라미터 대입을 통한 마이페이지 개인 프로필 세부 데이터 인출 가공 함수
     */
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
}