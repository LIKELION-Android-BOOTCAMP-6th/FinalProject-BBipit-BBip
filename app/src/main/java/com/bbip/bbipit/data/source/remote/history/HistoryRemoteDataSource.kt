package com.bbip.bbipit.data.source.remote.history

import com.bbip.bbipit.data.source.model.HistoryDto
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.entity.HistoryComment
import kotlinx.coroutines.flow.Flow

interface HistoryRemoteDataSource {
    // 히스토리 서버 저장
    suspend fun saveHistory(requestDto: HistoryDto): String
    // 히스토리 삭제
    suspend fun deleteHistory(historyId: String): Boolean
    // 히스토리 사진 업로드
    suspend fun uploadHistoryImages(uid: String, historyId: String, images: List<ByteArray>): List<String>
    // 히스토리 코멘트 등록
    suspend fun addHistoryComment(historyId: String, text: String): String
    // 히스토리 좋아요 등록
    suspend fun toggleHistoryLike(historyId: String): Boolean
    // 히스토리 코멘트 구독
    fun observeHistoryComments(historyId: String): Flow<List<HistoryComment>>
    // 히스토리 구독 (단일)
    fun observeHistoriesByUidList(uidList: List<String>): Flow<List<History>>
    suspend fun updateHistory(historyId: String, content: String): Boolean
}