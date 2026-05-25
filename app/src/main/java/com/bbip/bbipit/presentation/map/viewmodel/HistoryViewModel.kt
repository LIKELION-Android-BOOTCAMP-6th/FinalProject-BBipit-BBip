package com.bbip.bbipit.presentation.map.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

// 히스토리 화면 상태
data class HistoryState(
    val isLoading: Boolean = false,
    val nearbyHistories: List<History> = emptyList(),
    val errorMessage: String? = null,
    val currentLat: Double = 37.5665,
    val currentLng: Double = 126.9780
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : BaseViewModel<HistoryState>(HistoryState()) {

    // 로컬 메모리 캐시
    private val historyCache = mutableMapOf<String, History>()

    // 히스토리 생성
    fun createNewHistory(
        category: String,
        placeName: String,
        content: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            val result = historyRepository.saveMyHistory(
                category = category,
                placeName = placeName,
                content = content,
                latitude = latitude,
                longitude = longitude
            )

            result.onSuccess { documentId ->
                updateState {
                    copy(
                        isLoading = false,
                        currentLat = latitude,
                        currentLng = longitude
                    )
                }
                refreshHistory()

                Log.d("HistoryViewModel", "히스토리가 성공적으로 만들어졌습니다! DocID: $documentId")
            }.onFailure { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = error.message ?: "히스토리를 남기지 못했습니다."
                    )
                }
            }
        }
    }

    // 주변 히스토리 목록 조회
    suspend fun fetchNearbyHistory(lat: Double, lng: Double) {
        updateState {
            copy(
                isLoading = true,
                errorMessage = null,
                currentLat = lat,
                currentLng = lng
            )
        }

        val result = historyRepository.fetchNearbyHistory(lat, lng)

        result.onSuccess { newHistories ->
            clearCache()

            newHistories.forEach { history ->
                val historyId = history.id.ifEmpty { "${history.userId}_${history.createdAt}" }
                historyCache[historyId] = history

                Log.d("HistoryViewModel", history.toString())
            }

            updateState {
                copy(
                    isLoading = false,
                    nearbyHistories = historyCache.values.toList()
                )
            }
            Log.d("HistoryViewModel", "새 지역 이동 동기화 완료. 현재 반경 내 히스토리 개수: ${historyCache.size}")
        }.onFailure { error ->
            updateState {
                copy(
                    isLoading = false,
                    errorMessage = error.message ?: "주변 히스토리 불러오는데 실패했습니다."
                )
            }
        }
    }

    // 히스토리 목록 갱신
    fun refreshHistory() {
        viewModelScope.launch {
            fetchNearbyHistory(currentState.currentLat, currentState.currentLng)
        }
    }

    // 로컬 캐시 초기화
    fun clearCache() {
        historyCache.clear()
        updateState { copy(nearbyHistories = emptyList()) }
    }

    // 히스토리 삭제
    fun deleteHistory(historyId: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            val result = historyRepository.deleteHistory(historyId)

            result.onSuccess {
                updateState { copy(isLoading = false) }
                refreshHistory()
                Log.d("HistoryViewModel", "🎯 히스토리가 성공적으로 삭제되었습니다. ID: $historyId")
            }.onFailure { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = error.message ?: "히스토리를 삭제하지 못했습니다."
                    )
                }
            }
        }
    }
}