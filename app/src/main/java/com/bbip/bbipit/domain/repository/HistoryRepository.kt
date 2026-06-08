package com.bbip.bbipit.domain.repository

import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.entity.HistoryComment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    // 히스토리 서버 저장
    suspend fun saveMyHistory(
        category: String,
        placeName: String,
        content: String,
        latitude: Double,
        longitude: Double,
        images: List<ByteArray>
    ): Result<String>

    // 히스토리 삭제
    suspend fun deleteHistory(historyId: String): Result<Unit>
    // 히스토리 추가
    suspend fun addHistoryComment(historyId: String, text: String): Result<String>
    // 히스토리 코멘트 구독
    fun observeHistoryComments(historyId: String): Flow<List<HistoryComment>>
    // 뷰모델 또는 화면 레이어가 캐싱된 전역 스트림을 구독할 때 사용하는 함수
    fun observeSharedHistories(): Flow<List<History>>
    // 히스토리 구독(나+친구)
    @OptIn(ExperimentalCoroutinesApi::class)
    fun startSharedHistoryObservation(myUid: String)
    // 히스토리 좋아요 토글 (추가/취소)
    suspend fun toggleHistoryLike(historyId: String): Result<Unit>
}