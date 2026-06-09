package com.bbip.bbipit.data.source.remote.history

import com.bbip.bbipit.data.source.model.HistoryRequestDto
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.entity.HistoryComment
import kotlinx.coroutines.flow.Flow

interface HistoryRemoteDataSource {
    // 히스토리 서버 저장
    suspend fun saveHistory(requestDto: HistoryRequestDto): String

    // 주변 히스토리 목록 조회
//    suspend fun fetchNearbyHistory(centerLat: Double, centerLng: Double): Map<*, *>

    // 히스토리 삭제
    suspend fun deleteHistory(historyId: String): Boolean
    suspend fun uploadHistoryImages(uid: String, images: List<ByteArray>): List<String>
    suspend fun addHistoryComment(historyId: String, text: String): String
    fun observeHistoryComments(historyId: String): Flow<List<HistoryComment>>
    fun observeHistoriesByUidList(uidList: List<String>): Flow<List<History>>
}