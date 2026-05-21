package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.LiveStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 실시간 사용자 상태 정보 갱신, 단발성 조회 및 스트림 구독 흐름 제어 추상화 도메인 리포지토리 인터페이스
 */
interface LiveStatusRepository {
    // 프레젠테이션 레이어 컴포넌트 전역 관찰용 내 실시간 상태 공유 데이터 플로우
    val myLiveStatusFlow: StateFlow<LiveStatus?>

    // 관계 수립 친구 실시간 위치 지리 정보 변동 내역 상시 추적 관찰 전역 상태 플로우
    val friendsLiveStatusFlow: StateFlow<List<LiveStatus>>

    // 내 고유 UID 기준 인접 친구 목록 탐색 및 실시간 위치 스트림 백그라운드 활성화 함수
    fun observeFriendsLiveStatus(myUid: String)

    // 특정 단일 사용자 원격 데이터베이스 실시간 상태 기록면 모니터링 구독 데이터 흐름 함수
    fun observeUserLiveStatus(uid: String): Flow<Result<LiveStatus>>

    // 변경 내 실시간 위치 세트 및 상태 명세 원격 서버 영역 동기화 상향 전송 함수
    suspend fun updateMyLiveStatus(liveStatus: LiveStatus): Result<Unit>

    // 특정 사용자 UID 타겟 지정 기반 현재 물리 상태 명세 원격지 단발성 다이렉트 조회 함수
    suspend fun getLiveStatusByUid(targetUid: String): Result<LiveStatus>

    // 로컬 메모리 계층 적재 상주 내 수명 주기 상태 캐시 스냅샷 동기 즉시 인출 함수
    fun getCachedMyLiveStatus(): LiveStatus?

    // 현재 진입 특정 채팅방 고유 식별 명세 토대 서버 공간 세션 주기 생존 신호(Heartbeat) 송신 함수
    suspend fun updateLifeCycle(currentRoomId: String?): Result<Unit>
}