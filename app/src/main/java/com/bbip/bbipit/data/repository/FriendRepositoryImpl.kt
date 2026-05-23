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
 * 친구 데이터 처리
 */
@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val friendRemoteDataSource: FriendRemoteDataSource,
    private val firestore: FirebaseFirestore,
): FriendRepository {

    // 친구 목록 저장 및 공유용 캐시 Flow
    private val _myFriends = MutableStateFlow<List<Friend>>(emptyList())
    override val myFriends: StateFlow<List<Friend>> = _myFriends.asStateFlow()

    // 실시간 데이터 변경 감지 리스너
    private var friendsListener: ListenerRegistration? = null

    /**
     * 친구 목록 실시간 구독 함수
     */
    override fun startObservingFriends(myUid: String) {
        friendsListener?.remove()

        // 친구 목록 저장소 구독 및 데이터 갱신
        friendsListener = firestore.collection("Users").document(myUid)
            .collection("Friendships")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("관제탑 서비스", "❌ [친구 목록 동기화 실패]", e)
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                // 데이터 변환 및 ID 보정
                val friendsList = snapshot.documents.mapNotNull { doc ->
                    val dto = doc.data.toFriendshipDto()

                    val finalDto = if (dto.uid.isEmpty()) {
                        dto.copy(uid = doc.id)
                    } else {
                        dto
                    }

                    finalDto.toDomain()
                }

                _myFriends.value = friendsList

                Log.d("관제탑 서비스", "🔄 [친구 목록 동기화됨] 현재 위치 추적 친구: ${friendsList.size}명")
            }
    }

    /**
     * 친구 목록 구독 해제 함수
     */
    fun stopObservingFriends() {
        friendsListener?.remove()
        friendsListener = null
    }

    /**
     * 친구 요청 전송 함수
     */
    override suspend fun sendFriendRequest(targetCode: String): Result<String> {
        return try {
            // 서버에 친구 요청 등록
            val message = friendRemoteDataSource.sendFriendRequest(targetCode)
            Result.Success(message)
        } catch (e: Exception) {
            // 요청 실패 예외 처리
            Log.e("UserRepository", "친구 요청 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 요청 중 오류 발생"))
        }
    }

    /**
     * 친구 삭제 함수
     */
    override suspend fun deleteFriend(targetUid: String): Result<String> {
        return try {
            // 서버에서 친구 관계 삭제
            val message = friendRemoteDataSource.deleteFriend(targetUid)
            Result.Success(message)
        } catch (e: Exception) {
            // 삭제 실패 예외 처리
            Log.e("UserRepository", "친구 삭제 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 삭제 중 오류 발생"))
        }
    }

    /**
     * 수락된 친구 목록 조회 함수
     */
    override suspend fun getMyAcceptedFriends(): Result<List<Friend>> {
        return try {
            // 친구 목록 데이터 가져오기
            val friends = friendRemoteDataSource.getMyAcceptedFriends()
            Result.Success(friends)
        } catch (e: Exception) {
            // 조회 실패 예외 처리
            Log.e("UserRepository", "친구 목록 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 목록을 가져오지 못했습니다."))
        }
    }

    /**
     * 친구 요청 수락 함수
     */
    override suspend fun acceptFriendRequest(targetUid: String): Result<Boolean> {
        return try {
            // 친구 요청 수락 상태로 변경
            val isSuccess = friendRemoteDataSource.acceptFriendRequest(targetUid)
            Result.Success(isSuccess)
        } catch (e: Exception) {
            // 수락 실패 예외 처리
            Log.e("UserRepository", "친구 수락 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 수락 실패"))
        }
    }

    /**
     * 친구 요청 거절 함수
     */
    override suspend fun declineFriendRequest(targetUid: String): Result<Boolean> {
        return try {
            // 친구 요청 거절 및 삭제
            val isSuccess = friendRemoteDataSource.declineFriendRequest(targetUid)
            Result.Success(isSuccess)
        } catch (e: Exception) {
            // 거절 실패 예외 처리
            Log.e("UserRepository", "친구 거절 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 거절 실패"))
        }
    }

    /**
     * 받은 친구 요청 대기 목록 조회 함수
     */
    override suspend fun getPendingFriendRequests(): Result<List<User>> {
        return try {
            // 대기 중인 요청 목록 가져오기
            val friends = friendRemoteDataSource.getPendingFriendRequests()
            Result.Success(friends)
        } catch (e: Exception) {
            // 조회 실패 예외 처리
            Result.Failure(AppError.Unknown("요청 목록을 불러오는 데 실패했습니다."))
        }
    }

    /**
     * 친구 프로필 및 관계 상태 조회 함수
     */
    override suspend fun getFriendProfileWithStatus(targetUid: String): Result<Pair<User, String>> {
        return try {
            // 프로필 정보 및 친구 상태 데이터 가져오기
            val response = friendRemoteDataSource.getFriendProfileWithStatus(targetUid)
            val profileMap = response["profile"] as? Map<String, Any> ?: throw Exception("친구 정보를 찾을 수 없습니다.")
            val user = profileMap.toDomain()
            val friendshipStatus = response["friendship_status"] as? String ?: "none"
            Result.Success(Pair(user, friendshipStatus))
        } catch (e: Exception) {
            // 조회 실패 예외 처리
            Log.e("UserRepository", "친구 프로필 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "친구 상태 조회 실패"))
        }
    }
}