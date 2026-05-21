package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.mapper.toDomain
import com.bbip.bbipit.data.mapper.toFriendshipDto
import com.bbip.bbipit.data.source.model.FriendshipDto
import com.bbip.bbipit.data.source.remote.friend.FriendRemoteDataSource
import com.bbip.bbipit.domain.entity.Friend
import com.bbip.bbipit.domain.entity.User
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.FriendRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 친구 관련 데이터 처리


 */
@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val friendRemoteDataSource: FriendRemoteDataSource,
    private val firestore: FirebaseFirestore,
): FriendRepository {

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
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("관제탑 서비스", "❌ [친구 목록 동기화 실패]", e)
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val friendsList = snapshot.documents.mapNotNull { doc ->
                    // Map 매퍼 함수를 통해 안전하게 스네이크 케이스 필드 파싱
                    val dto = doc.data.toFriendshipDto()

                    // 만약 문서 내부 필드가 비어있다면 문서 ID({friend_uid})를 식별자로 강제 보정
                    val finalDto = if (dto.uid.isEmpty()) {
                        dto.copy(uid = doc.id)
                    } else {
                        dto
                    }

                    finalDto.toDomain()
                }

                // 데이터 발행
                _myFriends.value = friendsList

                // 콜백 내부에서 로그 출력하여 실시간 상태 추적 보장
                Log.d("관제탑 서비스", "🔄 [친구 목록 동기화됨] 현재 위치 추적 친구: ${friendsList.size}명")
            }
    }

    // 앱 종료 및 서비스 소멸 시점 메모리 누수 제어 목적의 실시간 파이어베이스 리스너 철거 함수
    fun stopObservingFriends() {
        friendsListener?.remove()
        friendsListener = null
    }

    /**
     * 타인 고유 식별자 타겟 신규 친구 요청 관계의 파이어베이스 업로드 개시 함수
     */
    override suspend fun sendFriendRequest(targetCode: String): Result<String> {
        return try {
            val message = friendRemoteDataSource.sendFriendRequest(targetCode)
            Result.Success(message)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 요청 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 요청 중 오류 발생"))
        }
    }

    /**
     * 기존 관계망 포함 특정 사용자 탐색 및 친구 목록 내 완전 제거 차단 함수
     */
    override suspend fun deleteFriend(targetUid: String): Result<String> {
        return try {
            val message = friendRemoteDataSource.deleteFriend(targetUid)
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
            val friends = friendRemoteDataSource.getMyAcceptedFriends()
            Result.Success(friends)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 목록 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 목록을 가져오지 못했습니다."))
        }
    }

    /**
     * 인입 특정 사용자의 친구 요청 내역 대상 수락 의사 확정 수립 함수
     */
    override suspend fun acceptFriendRequest(targetUid: String): Result<Boolean> {
        return try {
            val isSuccess = friendRemoteDataSource.acceptFriendRequest(targetUid)
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
            val isSuccess = friendRemoteDataSource.declineFriendRequest(targetUid)
            Result.Success(isSuccess)
        } catch (e: Exception) {
            Log.e("UserRepository", "친구 거절 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 거절 실패"))
        }
    }

    // 추가 요청 목록 조회
    override suspend fun getPendingFriendRequests(): Result<List<User>> {
        return try {
            // userRemoteDataSource를 통해 Firestore에서 status == "requested"인 리스트를 가져오는 함수 호출
            val friends = friendRemoteDataSource.getPendingFriendRequests()
            Result.Success(friends)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown("요청 목록을 불러오는 데 실패했습니다."))
        }
    }

    /**
     * 친구 프로필 상세 데이터 조회 및 쌍방 설정 친구 상태 세부 등급 문자열 코드 쌍 병합 복원 함수
     */
    override suspend fun getFriendProfileWithStatus(targetUid: String): Result<Pair<User, String>> {
        return try {
            val response = friendRemoteDataSource.getFriendProfileWithStatus(targetUid)
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