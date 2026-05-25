package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.mapper.HistoryMapper.toDomainHistory
import com.bbip.bbipit.data.source.model.HistoryRequestDto
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.data.source.remote.history.HistoryRemoteDataSource
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.HistoryRepository
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.functions.FirebaseFunctionsException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val remoteDataSource: HistoryRemoteDataSource
) : HistoryRepository {

    // 히스토리 서버 저장
    override suspend fun saveMyHistory(
        category: String,
        placeName: String,
        content: String,
        latitude: Double,
        longitude: Double): Result<String> {
        return try {
            val requestDto = HistoryRequestDto(
                category = category,
                placeName = placeName,
                content = content,
                latitude = latitude,
                longitude = longitude,
            )

            val result = remoteDataSource.saveHistory(requestDto)
            Log.d("HistoryRepositoryImpl", result)
            Result.Success(result)
        } catch (e: FirebaseFunctionsException) {
            Result.Failure(AppError.Unknown("[서버 코드 ${e.code}]: ${e.message}"))
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown(e.message ?: "히스토리 서버 저장 도중 오류 발생"))
        }
    }

    // 주변 히스토리 목록 조회
    override suspend fun fetchNearbyHistory(
        centerLat: Double,
        centerLng: Double
    ): Result<List<History>> {
        return try {
            val rawResultMap = remoteDataSource.fetchNearbyHistory(centerLat, centerLng)
            val historiesList = rawResultMap["histories"] as? List<*> ?: emptyList<Any>()

            // 응답 객체 리스트를 도메인 엔티티 리스트로 변환
            val matchingHistories = historiesList.mapNotNull { item ->
                (item as? Map<*, *>)?.toDomainHistory()
            }

            Result.Success(matchingHistories)
        } catch (e: FirebaseFunctionsException) {
            Result.Failure(AppError.Unknown("[서버 반경 에러 ${e.code}]: ${e.message}"))
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown(e.message ?: "주변 히스토리를 불러오지 못했습니다."))
        }
    }

    // 히스토리 삭제
    override suspend fun deleteHistory(historyId: String): Result<Unit> {
        return try {
            val success = remoteDataSource.deleteHistory(historyId)
            if (success) {
                Result.Success(Unit)
            } else {
                Result.Failure(AppError.Unknown("히스토리 삭제에 실패했습니다."))
            }
        } catch (e: FirebaseFunctionsException) {
            Result.Failure(AppError.Unknown("[서버 코드 ${e.code}]: ${e.message}"))
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown(e.message ?: "히스토리 삭제 도중 오류 발생"))
        }
    }
}