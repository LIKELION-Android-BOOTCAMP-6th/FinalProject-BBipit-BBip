package com.bbip.bbipit.domain.usecase

import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.core.result.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncMyLocationUseCase @Inject constructor(
    private val liveStatusRepository: LiveStatusRepository
) {
    private var locationSyncCounter = 0

    suspend operator fun invoke(liveStatus: LiveStatus): Result<Unit> {
        val result = liveStatusRepository.updateMyLiveStatus(liveStatus)

        if (result is Result.Success) {
            // 위치 동기화 성공 시점 기준 동기화 카운터 누적 처리
            locationSyncCounter++

            // 특정 주기(5초 x 6회 = 30초) 진입 감지 목적의 조건문
            if (locationSyncCounter >= 6) {
                locationSyncCounter = 0 // 카운터 초기화
                liveStatusRepository.updateLifeCycle(currentRoomId = liveStatus.currentRoomId)
            }
        }
        return result
    }
}