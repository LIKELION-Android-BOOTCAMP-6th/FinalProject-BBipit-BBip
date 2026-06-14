package com.bbip.bbipit.data.source.remote.live

import com.bbip.bbipit.data.source.model.LiveStatusDto
import kotlinx.coroutines.flow.Flow

/**
 * 실시간 상태 및 위치 정보 통신 원격 데이터 소스 인터페이스
 */
interface LiveStatusRemoteDataSource {
    suspend fun updateLifeCycle(currentRoomId: String?)
    fun updateMyLiveLocation(uid: String, latitude: Double, longitude: Double)
    fun observeUserLiveStatus(uid: String): Flow<Pair<LiveStatusDto, Boolean>>
    suspend fun getLiveStatusByUid(targetUid: String): LiveStatusDto
}