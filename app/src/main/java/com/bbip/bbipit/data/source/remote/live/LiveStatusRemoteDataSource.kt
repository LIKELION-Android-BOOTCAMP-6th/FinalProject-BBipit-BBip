package com.bbip.bbipit.data.source.remote.live

import com.bbip.bbipit.data.source.model.LiveStatusDto
import kotlinx.coroutines.flow.Flow

/**
 * 파이어베이스 원격 데이터베이스 및 클라우드 함수 서버 간 실시간 상태 통신 정의 원격 데이터 소스 인터페이스
 */
interface LiveStatusRemoteDataSource {
    suspend fun updateLifeCycle(currentRoomId: String?)
    fun updateMyLiveStatus(uid: String, dto: LiveStatusDto)
    fun observeUserLiveStatus(uid: String): Flow<Pair<LiveStatusDto, Boolean>>
    suspend fun getLiveStatusByUid(targetUid: String): LiveStatusDto
}