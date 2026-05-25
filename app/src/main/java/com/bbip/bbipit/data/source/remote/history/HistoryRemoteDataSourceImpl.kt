package com.bbip.bbipit.data.source.remote.history

import android.util.Log
import com.bbip.bbipit.data.source.model.HistoryRequestDto
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRemoteDataSourceImpl @Inject constructor(
    private val functions: FirebaseFunctions
): HistoryRemoteDataSource {

    // 히스토리 서버 저장
    override suspend fun saveHistory(requestDto: HistoryRequestDto): String {
        val result = functions
            .getHttpsCallable("createHistory")
            .call(requestDto.toMap())
            .await()

        val responseData = result.data as? Map<*, *>
        return responseData?.get("documentId") as? String
            ?: throw IllegalStateException("서버가 생성된 문서 ID를 반환하지 않았습니다.")
    }

    // 주변 히스토리 목록 조회
    override suspend fun fetchNearbyHistory(centerLat: Double, centerLng: Double): Map<*, *> {
        val data = hashMapOf(
            "centerLat" to centerLat,
            "centerLng" to centerLng
        )
        val result = functions
            .getHttpsCallable("fetchNearbyHistory")
            .call(data)
            .await()

        return result.data as? Map<*, *> ?: emptyMap<Any, Any>()
    }

    // 히스토리 삭제
    override suspend fun deleteHistory(historyId: String): Boolean {
        val data = hashMapOf("historyId" to historyId)
        val result = functions
            .getHttpsCallable("deleteHistory")
            .call(data)
            .await()

        val responseData = result.data as? Map<*, *>
        return responseData?.get("success") as? Boolean ?: false
    }
}