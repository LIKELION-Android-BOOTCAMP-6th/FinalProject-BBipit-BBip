package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.LiveStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 실시간 상태 및 위치 정보 관련 Repository 인터페이스
 */
interface LiveStatusRepository {

    // 내 라이브 상태 Flow
    val myLiveStatusFlow: StateFlow<LiveStatus?>

    // 친구들의 라이브 상태 목록 Flow
    val friendsLiveStatusFlow: StateFlow<List<LiveStatus>>

    /**
     * 친구 목록 기반 실시간 위치 구독 함수
     */
    fun observeFriendsLiveStatus(myUid: String)

    /**
     * 특정 유저의 라이브 상태 변화를 구독(관찰)하는 Flow 생성 함수
     */
    fun observeUserLiveStatus(uid: String): Flow<Result<LiveStatus>>

    /**
     * 내 위치 및 상태 정보를 원격 서버에 업데이트하는 함수
     */
    suspend fun updateMyLiveStatus(liveStatus: LiveStatus): Result<Unit>

    /**
     * 특정 유저의 상태 정보를 1회성으로 조회하는 함수
     */
    suspend fun getLiveStatusByUid(targetUid: String): Result<LiveStatus>

    /**
     * 메모리 캐시에 저장된 내 라이브 상태 반환 함수
     */
    fun getCachedMyLiveStatus(): LiveStatus?

    /**
     * 유저 온라인 상태 및 현재 채팅방 정보 업데이트 함수
     */
    suspend fun updateLifeCycle(currentRoomId: String?): Result<Unit>
    suspend fun updateLocationSharingState(isSharing: Boolean): Result<Unit>
    fun observeLocationSharingState(): Flow<Boolean>
}