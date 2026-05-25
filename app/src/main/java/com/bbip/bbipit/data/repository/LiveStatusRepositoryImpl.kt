package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.mapper.toDomain
import com.bbip.bbipit.data.mapper.toDto
import com.bbip.bbipit.data.source.model.LiveStatusDto
import com.bbip.bbipit.data.source.remote.live.LiveStatusRemoteDataSource
import com.bbip.bbipit.data.source.remote.user.UserRemoteDataSourceImpl
import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.FriendRepository
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 및 친구 실시간 위치·접속 상태 관리
 */
@Singleton
class LiveStatusRepositoryImpl @Inject constructor(
    private val liveStatusRemoteDataSource: LiveStatusRemoteDataSource,
    private val friendRepository: FriendRepository,
) : LiveStatusRepository {

    // 비동기 작업 처리용 Scope
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // 내 라이브 상태 저장 및 공유용 캐시 Flow
    private val _myLiveStatusFlow = MutableStateFlow<LiveStatus?>(null)
    override val myLiveStatusFlow: StateFlow<LiveStatus?> = _myLiveStatusFlow.asStateFlow()

    // 친구들의 라이브 상태 목록 저장 및 공유용 캐시 Flow
    private val _friendsLiveStatusFlow = MutableStateFlow<List<LiveStatus>>(emptyList())
    override val friendsLiveStatusFlow: StateFlow<List<LiveStatus>> = _friendsLiveStatusFlow.asStateFlow()

    private val _isLocationSharingEnabled = MutableStateFlow(true) // 기본값 true

    /**
     * 친구 목록을 기반으로 개별 위치를 실시간 구독하는 함수
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFriendsLiveStatus(myUid: String) {
        friendRepository.myFriends
            .flatMapLatest { friends ->
                // 수락 완료된 친구 필터링 및 ID 추출
                val acceptedFriends = friends.filter { it.friendshipStatus == "accepted" }
                val friendUids = acceptedFriends.map { it.uid }

                if (friendUids.isEmpty()) {
                    flowOf(emptyList<LiveStatus>())
                } else {
                    // 친구 전원의 실시간 상태 관찰 Flow 생성
                    val friendFlows = friendUids.map { uid ->
                        observeUserLiveStatus(uid)
                            .map { result ->
                                when (result) {
                                    is Result.Success -> result.data
                                    is Result.Failure -> null
                                }
                            }
                    }
                    // 개별 Flow들을 하나의 리스트로 통합
                    combine(friendFlows) { statuses -> statuses.filterNotNull() }
                }
            }
            .onEach { updatedList ->
                // 캐시 Flow 갱신
                _friendsLiveStatusFlow.value = updatedList
                Log.d("관제탑 서비스", "🔄 [친구 위치 동기화됨] 현재 위치 추적 친구: ${updatedList.size}명")
            }
            .launchIn(repositoryScope)
    }

    /**
     * 내 위치 및 상태 정보를 원격 서버에 업데이트하는 함수
     */
    override suspend fun updateMyLiveStatus(liveStatus: LiveStatus): Result<Unit> {
        return try {
            // 메모리 캐시 선제 갱신
            _myLiveStatusFlow.value = liveStatus

            // 현재 위치 공유가 켜져있는지 확인
            observeLocationSharingState().first().let { isSharingEnabled ->
                if (isSharingEnabled) {
                    // 원격 저장소에 데이터 저장
                    liveStatusRemoteDataSource.updateMyLiveStatus(
                        uid = liveStatus.uid,
                        dto = liveStatus.toDto()
                    )
                } else {
                    Log.d("관제탑 서비스", "위치 공유가 비활성화되어 백그라운드 위치를 서버에 전송하지 않습니다.")
                }
            }


            Result.Success(Unit)
        } catch (e: Exception) {
            // 업데이트 실패 예외 처리
            Result.Failure(AppError.Unknown(e.message ?: "내 상태 업데이트 실패"))
        }
    }

    /**
     * 유저 온라인 상태 및 현재 채팅방 정보 업데이트 함수
     */
    override suspend fun updateLifeCycle(currentRoomId: String?): Result<Unit> {
        return try {
            // 서버에 활성 상태 전송
            liveStatusRemoteDataSource.updateLifeCycle(currentRoomId)
            Result.Success(Unit)
        } catch (e: Exception) {
            // 전송 실패 예외 처리
            Result.Failure(AppError.Unknown(e.message ?: "Heartbeat 실패"))
        }
    }

    /**
     * 특정 유저의 상태 정보를 1회성으로 조회하는 함수
     */
    override suspend fun getLiveStatusByUid(targetUid: String): Result<LiveStatus> {
        return try {
            // 원격 데이터 조회 및 도메인 엔티티 변환
            val dto = liveStatusRemoteDataSource.getLiveStatusByUid(targetUid)
            val domainEntity = dto.toDomain(uid = targetUid, isFromCache = false)

            _isLocationSharingEnabled.value = dto.isSharing
            Result.Success(domainEntity)
        } catch (e: Exception) {
            // 조회 실패 예외 처리
            Result.Failure(AppError.Unknown(e.message ?: "실시간 라이브 정보 조회 실패"))
        }
    }

    /**
     * 메모리 캐시에 저장된 내 라이브 상태 반환 함수
     */
    override fun getCachedMyLiveStatus(): LiveStatus? = _myLiveStatusFlow.value

    /**
     * 특정 유저의 라이브 상태 변화를 구독(관찰)하는 Flow 생성 함수
     */
    override fun observeUserLiveStatus(uid: String): Flow<Result<LiveStatus>> {
        return liveStatusRemoteDataSource.observeUserLiveStatus(uid)
            .map<Pair<LiveStatusDto, Boolean>, Result<LiveStatus>> { (dto, isFromCache) ->
                // 데이터 수신 후 도메인 엔티티로 변환하여 반환
                val domainEntity = dto.toDomain(uid, isFromCache)
                Result.Success(domainEntity)
            }
            .catch { exception ->
                // 구독 실패 예외 처리 및 에러 전달
                emit(Result.Failure(AppError.Unknown(exception.message ?: "라이브 상태 구독 실패")))
            }
    }

    override fun observeLocationSharingState(): Flow<Boolean> = _isLocationSharingEnabled.asStateFlow()

    override suspend fun updateLocationSharingState(isSharing: Boolean): Result<Unit> {
        return try {
            _isLocationSharingEnabled.value = isSharing

            val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.Failure(AppError.Unknown("로그인 정보 없음"))

            FirebaseFirestore.getInstance().collection("Users").document(myUid)
                .update("is_sharing", isSharing)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown(e.message ?: "위치 공유 상태 업데이트 실패"))
        }
    }
}