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
    suspend fun addHistoryComment(historyId: String, text: String): Result<String>
    fun observeHistoryComments(historyId: String): Flow<List<HistoryComment>>
    // 백그라운드 서비스가 상주 구독을 시작할 때 호출하는 함수

    // 뷰모델 또는 화면 레이어가 캐싱된 전역 스트림을 구독할 때 사용하는 함수
    fun observeSharedHistories(): Flow<List<History>>
    @OptIn(ExperimentalCoroutinesApi::class)
    fun startSharedHistoryObservation(myUid: String)
}