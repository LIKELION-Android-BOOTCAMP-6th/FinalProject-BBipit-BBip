package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.History

interface HistoryRepository {
    // 히스토리 서버 저장
    suspend fun saveMyHistory(
        category: String,
        placeName: String,
        content: String,
        latitude: Double,
        longitude: Double
    ): Result<String>

    // 주변 히스토리 목록 조회
    suspend fun fetchNearbyHistory(centerLat: Double, centerLng: Double): Result<List<History>>

    // 히스토리 삭제
    suspend fun deleteHistory(historyId: String): Result<Unit>
}