package com.bbip.bbipit.presentation.map.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.BaseViewModel
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.entity.HistoryComment
import com.bbip.bbipit.domain.repository.AuthRepository
import com.bbip.bbipit.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// 히스토리 화면 상태 데이터 모델
data class HistoryUiState(
    val isLoading: Boolean = false,
    val histories: List<History> = emptyList(),
    val errorMessage: String? = null,
    val currentLat: Double = 37.5665,
    val currentLng: Double = 126.9780,
    val selectedImages: List<ByteArray> = emptyList(),
    val currentComments: List<HistoryComment> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val authRepository: AuthRepository,
) : BaseViewModel<HistoryUiState>(HistoryUiState()) {
    private var historyStreamJob: Job? = null
    private var commentStreamJob: Job? = null

    private val TAG = "HistoryViewModel"

    // 특정 히스토리 데이터 수정 요청
    fun updateHistory(historyId: String, content: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            val result = historyRepository.updateHistory(
                historyId = historyId,
                content = content
            )

            result.onSuccess {
                updateState { copy(isLoading = false) }
                Log.d(TAG, "🎯 히스토리가 성공적으로 수정되었습니다. ID: $historyId")
            }.onFailure { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = error.message ?: "히스토리를 수정하지 못했습니다."
                    )
                }
            }
        }
    }

    // 히스토리 좋아요 토글 요청 함수 추가
    fun toggleHistoryLike(historyId: String) {
        viewModelScope.launch {
            val result = historyRepository.toggleHistoryLike(historyId)
            result.onFailure { error ->
                updateState { copy(errorMessage = error.message ?: "좋아요 반영 실패") }
            }
        }
    }

    // 공유 히스토리 캐시 스트림 관측 및 UI 상태 갱신
    fun startHistoryObservation() {
        historyStreamJob?.cancel()
        updateState { copy(isLoading = true) }

        historyStreamJob = viewModelScope.launch {
            historyRepository.observeSharedHistories()
                .catch { exception ->
                    updateState { copy(isLoading = false, errorMessage = exception.message) }
                }
                .collect { sharedHistories ->
                    updateState {
                        copy(
                            isLoading = false,
                            histories = sharedHistories
                        )
                    }
                    Log.d(TAG, "🔄 [UI 최적화 완료] 서비스가 캐싱한 데이터 ${sharedHistories.size}건을 화면에 매핑")
                }
        }
    }

    // 히스토리 관측 스트림 해제 및 자원 정리
    fun closeHistoryObservation() {
        historyStreamJob?.cancel()
        historyStreamJob = null
    }

    // 특정 히스토리의 실시간 댓글 스트림 관측 시작
    fun observeComments(historyId: String) {
        commentStreamJob?.cancel()

        commentStreamJob = viewModelScope.launch {
            historyRepository.observeHistoryComments(historyId).collect { commentsList ->
                updateState { copy(currentComments = commentsList) }
                Log.d(TAG, "📢 [실시간 댓글 스트림 수신] 총 ${commentsList.size}건 반영")
            }
        }
    }

    // 댓글 관측 스트림 해제 및 데이터 초기화
    fun closeCommentsObservation() {
        commentStreamJob?.cancel()
        commentStreamJob = null
        updateState { copy(currentComments = emptyList()) }
    }

    // 히스토리 댓글 등록 요청
    fun addHistoryComment(historyId: String, text: String) {
        viewModelScope.launch {
            val result = historyRepository.addHistoryComment(historyId, text)
            result.onSuccess { commentId ->
                Log.d(TAG, "🎯 댓글 서버 등록 정상 확정 완료! ID: $commentId")
            }.onFailure { error ->
                updateState { copy(errorMessage = error.message ?: "댓글 등록 실패") }
            }
        }
    }

    // 첨부 이미지 리스트 상태 업데이트 (최대 3장 제한)
    fun updateSelectedImages(images: List<ByteArray>) {
        updateState { copy(selectedImages = images.take(3)) }
    }

    // 첨부 이미지 데이터 전체 비우기
    fun clearSelectedImages() {
        updateState { copy(selectedImages = emptyList()) }
    }

    // 신규 히스토리 데이터 생성 및 서버 저장 요청
    fun createNewHistory(
        category: String,
        placeName: String,
        content: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            val imagesToUpload = currentState.selectedImages

            updateState { copy(isLoading = true) }

            val result = historyRepository.saveMyHistory(
                category = category,
                placeName = placeName,
                content = content,
                latitude = latitude,
                longitude = longitude,
                images = imagesToUpload
            )

            result.onSuccess { documentId ->
                updateState {
                    copy(
                        isLoading = false,
                        currentLat = latitude,
                        currentLng = longitude,
                        selectedImages = emptyList()
                    )
                }

                Log.d(TAG, "히스토리가 성공적으로 만들어졌습니다! DocID: $documentId")
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

    // 특정 히스토리 데이터 삭제 요청
    fun deleteHistory(historyId: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            val result = historyRepository.deleteHistory(historyId)

            result.onSuccess {
                updateState { copy(isLoading = false) }
                Log.d(TAG, "🎯 히스토리가 성공적으로 삭제되었습니다. ID: $historyId")
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

    fun getMyUid(): String {
        return authRepository.getCurrentUserUid() ?: ""
    }

    fun clearErrorMessage() {
        updateState { copy(errorMessage = null) }
    }
}