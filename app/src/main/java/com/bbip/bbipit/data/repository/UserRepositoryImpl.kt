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
    private val firestore: FirebaseFirestore,
) : UserRepository {

    // 수락 완료 상태의 내 친구 목록 배열 상시 저장 및 공유용 전역 캐시 플로우
    private val _myFriends = MutableStateFlow<List<Friend>>(emptyList())
    override val myFriends: StateFlow<List<Friend>> = _myFriends.asStateFlow()

    // 파이어베이스 데이터 변경 이벤트 수신용 실시간 스냅샷 리스너 등록 객체
    private var friendsListener: ListenerRegistration? = null

    /**
     * 지정 사용자의 수락 완료 친구 목록 서브 컬렉션 실시간 감시 및 로컬 메모리 상태 자동 동기화 함수
     * 중복 방지 목적의 기존 리스너 선제 파기 및 파이어베이스 스냅샷 리스너 재결합 처리
     */
    override fun startObservingFriends(myUid: String) {
        friendsListener?.remove()

        friendsListener = firestore.collection("Users").document(myUid)
            .collection("Friendships")
            .whereEqualTo("friendship_status", "accepted")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val friendsList = snapshot.documents.mapNotNull { doc ->
                    val dto = doc.toObject(FriendshipDto::class.java)

                    // 문서 데이터 내부 식별자 유실 시 Firestore 문서 ID를 고유 UID 값으로 강제 보정 처리
                    val finalDto = if (dto?.uid.isNullOrEmpty()) {
                        dto?.copy(uid = doc.id)
                    } else {
                        dto
                    }

                    finalDto?.toDomain()
                }
                _myFriends.value = friendsList
            }
    }

    // 앱 종료 및 서비스 소멸 시점 메모리 누수 제어 목적의 실시간 파이어베이스 리스너 철거 함수
    fun stopObservingFriends() {
        friendsListener?.remove()
        friendsListener = null
    }

    // 서버 영역 적재 기존 유효 푸시 서비스 토큰 코드 인출 함수
    override suspend fun getFcmToken(): String? = userRemoteDataSource.getToken()

    /**
     * 변경 프로필 내용 및 수신 토큰 상태의 데이터 소스 컴포넌트 경유 원격 갱신 함수
     */
    override suspend fun updateProfile(nickname: String?, status: String?, profileImageUrl: String?, fcmToken: String?): Result<String> {
        return try {
            val message = userRemoteDataSource.updateProfile(nickname, status, profileImageUrl, fcmToken)
            Result.Success(message)
        } catch (e: Exception) {
            Log.e("UserRepository", "프로필 업데이트 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알 수 없는 오류"))
        }
    }

    /**
     * 타인 고유 식별자 타겟 신규 친구 요청 관계의 파이어베이스 업로드 개시 함수
     */
    override suspend fun sendFriendRequest(targetUid: String): Result<String> {
        return try {
            val message = userRemoteDataSource.sendFriendRequest(targetUid)
            Result.Success(message)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 요청 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 요청 중 오류 발생"))
        }
    }

    // 추가 요청 목록 조회
    override suspend fun getPendingFriendRequests(): Result<List<User>> {
        return try {
            // userRemoteDataSource를 통해 Firestore에서 status == "requested"인 리스트를 가져오는 함수 호출
            val friends = userRemoteDataSource.getPendingFriendRequests()
            Result.Success(friends)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("요청 목록을 불러오는 데 실패했습니다."))
        }
    }

    /**
     * 기존 관계망 포함 특정 사용자 탐색 및 친구 목록 내 완전 제거 차단 함수
     */
    override suspend fun deleteFriend(targetUid: String): Result<String> {
        return try {
            val message = userRemoteDataSource.deleteFriend(targetUid)
            Result.Success(message)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 삭제 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 삭제 중 오류 발생"))
        }
    }

    /**
     * 수락 상태 친구 전원의 원본 프로필 데이터 세트 원격 저장소 기준 단발성 일괄 조회 호출 함수
     */
    override suspend fun getMyAcceptedFriends(): Result<List<Friend>> {
        return try {
            val friends = userRemoteDataSource.getMyAcceptedFriends()
            Result.Success(friends)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 목록 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 목록을 가져오지 못했습니다."))
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
     * 인입 특정 사용자의 친구 요청 내역 대상 수락 의사 확정 수립 함수
     */
    override suspend fun acceptFriendRequest(targetUid: String): Result<Boolean> {
        return try {
            val isSuccess = userRemoteDataSource.acceptFriendRequest(targetUid)
            Result.Success(isSuccess)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 수락 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 수락 실패"))
        }
    }

    /**
     * 수신 대기 상태 타인 친구 요청 명세 거절 및 관계 데이터베이스 내 즉시 삭제 함수
     */
    override suspend fun declineFriendRequest(targetUid: String): Result<Boolean> {
        return try {
            val isSuccess = userRemoteDataSource.declineFriendRequest(targetUid)
            Result.Success(isSuccess)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 거절 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 거절 실패"))
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

    /**
     * 친구 프로필 상세 데이터 조회 및 쌍방 설정 친구 상태 세부 등급 문자열 코드 쌍 병합 복원 함수
     */
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