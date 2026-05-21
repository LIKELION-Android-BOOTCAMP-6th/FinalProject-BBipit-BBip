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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 및 친구 실시간 위치·활성 상태 데이터 중앙 제어 및 메모리 캐싱 관리 도메인 리포지토리 구현체 클래스
 */
@Singleton
class LiveStatusRepositoryImpl @Inject constructor(
    private val  userRemoteDataSource: UserRemoteDataSourceImpl,
    private val liveStatusRemoteDataSource: LiveStatusRemoteDataSource,
    private val friendRepository: FriendRepository,
    private val firestore: FirebaseFirestore
) : LiveStatusRepository {

    // 데이터 소스 통신 및 흐름 구독 제어용 저장소 전역 비동기 코루틴 스코프
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // 앱 실행 주기 동안 메모리 상의 내 상태 동기화 유지 목적의 쓰기 가능 전역 캐시 플로우
    private val _myLiveStatusFlow = MutableStateFlow<LiveStatus?>(null)
    override val myLiveStatusFlow: StateFlow<LiveStatus?> = _myLiveStatusFlow.asStateFlow()

    // 친구 실시간 상태 배열 정보 전역 관찰 적재용 메모리 캐시 플로우
    private val _friendsLiveStatusFlow = MutableStateFlow<List<LiveStatus>>(emptyList())
    override val friendsLiveStatusFlow: StateFlow<List<LiveStatus>> = _friendsLiveStatusFlow.asStateFlow()

    /**
     * 친구 데이터 저장소 목록 실시간 구독 기반 개별 위치 관찰 흐름 동적 재구성 제어 함수
     * 변동 고유 식별자(UID) 목록 기준 개별 플로우 병합 처리를 통한 중앙 집중형 관측 데이터 세트 갱신 목적
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFriendsLiveStatus(myUid: String) {
        Log.d("테스트", "${friendRepository.myFriends.value}}")

        friendRepository.myFriends
            .flatMapLatest { friends ->
                val friendUids = friends.map { it.uid }

                if (friendUids.isEmpty()) {
                    flowOf(emptyList<LiveStatus>())
                } else {
                    // 유효 친구 목록 포함 인원 전원 대상 개별 실시간 파이어베이스 리스너 플로우 연계 개설
                    val friendFlows = friendUids.map { uid ->
                        observeUserLiveStatus(uid)
                            .map { result ->
                                when (result) {
                                    is Result.Success -> result.data
                                    is Result.Failure -> null
                                }
                            }
                    }
                    // 다중 데이터 흐름 결합 연산자 적용을 통한 널(Null) 객체 배제 유효 결과 배열 복원 처리
                    combine(friendFlows) { statuses -> statuses.filterNotNull() }
                }
            }
            .onEach { updatedList ->
                Log.d("관제탑 서비스", "🔄 [친구 목록 동기화됨] 현재 위치 추적 친구: ${updatedList.size}命")
                _friendsLiveStatusFlow.value = updatedList
            }
            .launchIn(repositoryScope)
    }

    /**
     * 내 최신 위치 및 상태 정보 원격 서버 전송 분기 동기화 함수
     * 실시간 연산 반응성 향상 목적의 물리 네트워크 트래픽 발생 직전 메모리 캐시 데이터 선제 갱신 처리
     */
    override suspend fun updateMyLiveStatus(liveStatus: LiveStatus): Result<Unit> {
        return try {
            _myLiveStatusFlow.value = liveStatus

            liveStatusRemoteDataSource.updateMyLiveStatus(
                uid = liveStatus.uid,
                dto = liveStatus.toDto()
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown(e.message ?: "내 상태 업데이트 실패"))
        }
    }

    /**
     * 현재 활성 상태 특정 채팅방 고유 식별자 전송 기반 세션 주기 신호(Heartbeat) 발생 함수
     */
    override suspend fun updateLifeCycle(currentRoomId: String?): Result<Unit> {
        return try {
            liveStatusRemoteDataSource.updateLifeCycle(currentRoomId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown(e.message ?: "Heartbeat 실패"))
        }
    }

    /**
     * 특정 사용자 UID 정보 조건 기준 원격 서버 단발성 상태 스냅샷 다이렉트 조회 함수
     * 물리 원격 스토리지 직통 값 반영을 위한 로컬 캐시 판별 변수 비활성화(false) 명시 처리
     */
    override suspend fun getLiveStatusByUid(targetUid: String): Result<LiveStatus> {
        return try {
            val dto = liveStatusRemoteDataSource.getLiveStatusByUid(targetUid)
            val domainEntity = dto.toDomain(uid = targetUid, isFromCache = false)
            Result.Success(domainEntity)
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown(e.message ?: "실시간 라이브 정보 조회 실패"))
        }
    }

    // 메모리 상주 중인 내 수명 주기 상태 캐시 스냅샷 동기 즉시 인출 함수
    override fun getCachedMyLiveStatus(): LiveStatus? = _myLiveStatusFlow.value

    /**
     * 타인 원격 데이터베이스 상태 기록면 실시간 연속 모니터링 및 가공 인출 흐름 생성 함수
     * 데이터 정합성 판단 기준 유지를 위한 로컬 내부 캐시 레이어 경유 여부 수반 처리
     */
    override fun observeUserLiveStatus(uid: String): Flow<Result<LiveStatus>> {
        return liveStatusRemoteDataSource.observeUserLiveStatus(uid)
            .map<Pair<LiveStatusDto, Boolean>, Result<LiveStatus>> { (dto, isFromCache) ->
                val domainEntity = dto.toDomain(uid, isFromCache)
                Result.Success(domainEntity)
            }
            .catch { exception ->
                emit(Result.Failure(AppError.Unknown(exception.message ?: "라이브 상태 구독 실패")))
            }
    }
}