package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.source.model.HistoryRequestDto
import com.bbip.bbipit.data.source.remote.auth.AuthRemoteDataSource
import com.bbip.bbipit.data.source.remote.history.HistoryRemoteDataSource
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.entity.HistoryComment
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.FriendRepository
import com.bbip.bbipit.domain.repository.HistoryRepository
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val remoteDataSource: HistoryRemoteDataSource,
    private val authDataSource: AuthRemoteDataSource,
    private val friendRepository: FriendRepository
) : HistoryRepository {

    // 최신 데이터 1개 캐시 및 다중 구독 가능한 전역 공유용 Hot Flow
    private val _sharedHistories = MutableSharedFlow<List<History>>(replay = 1)

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // 전역 구독 파이프라인 제어용 Job
    private var observationJob: kotlinx.coroutines.Job? = null

    // 백그라운드 서비스 시작 시 영구 구독을 실행하는 함수
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun startSharedHistoryObservation(myUid: String) {
        if (observationJob != null && observationJob?.isActive == true) return

        // 친구 목록 플로우를 기반으로 데이터 구독 제어
        observationJob = friendRepository.myFriends
            .map { friends ->
                // 수락 상태인 친구 UID 목록 추출
                friends.filter { it.friendshipStatus == "accepted" }
                    .map { it.uid }
            }
            .distinctUntilChanged() // 친구 목록 변경 시에만 리스너 재배정 수행
            .flatMapLatest { acceptedFriendUids ->
                Log.d("HistoryRepositoryImpl", "👥 [관계망 변동] 수락된 친구 수: ${acceptedFriendUids.size}명 (리스너 재배정)")

                // 실시간 검증을 위한 현재 사용자 UID 추출
                val currentMyUid = authDataSource.getCurrentUserUid().orEmpty()
                Log.d("HistoryRepositoryImpl", "📡 [리스너 동적 주입] 내 UID: $currentMyUid")

                // 내 히스토리 관측 Flow
                val myHistoryFlow = remoteDataSource.observeHistoriesByUidList(
                    if (currentMyUid.isNotEmpty()) listOf(currentMyUid) else emptyList()
                )

                // 친구 히스토리 관측 Flow (최대 30명 제한)
                val friendsHistoryFlow = remoteDataSource.observeHistoriesByUidList(
                    acceptedFriendUids.take(30)
                )

                // 내 히스토리와 친구 히스토리 병합 및 생성일 기준 내림차순 정렬
                combine(myHistoryFlow, friendsHistoryFlow) { myHistories, friendsHistories ->
                    (myHistories + friendsHistories).sortedByDescending { it.createdAt }
                }
            }
            .onEach { combinedHistories ->
                // 가공된 데이터를 SharedFlow 채널로 방출 (지도 마커 및 WearOS 전송용)
                _sharedHistories.emit(combinedHistories)
                Log.d("HistoryRepositoryImpl", "👣 [실시간 히스토리 동기화] 노출 가능 히스토리: ${combinedHistories.size}건")
            }
            .launchIn(repositoryScope)
    }

    // 뷰모델용 실시간 마커 동기화 채널 반환
    override fun observeSharedHistories(): Flow<List<History>> {
        return _sharedHistories.asSharedFlow()
    }

    // 특정 히스토리 댓글 목록 관측 Flow 반환
    override fun observeHistoryComments(historyId: String): Flow<List<HistoryComment>> {
        return remoteDataSource.observeHistoryComments(historyId)
    }

    // 히스토리 댓글 추가
    override suspend fun addHistoryComment(historyId: String, text: String): Result<String> {
        return try {
            // 빈 공백 댓글 검증 및 차단
            if (text.trim().isEmpty()) {
                return Result.Failure(AppError.Unknown("댓글 내용을 입력해 주세요."))
            }

            val commentId = remoteDataSource.addHistoryComment(historyId = historyId, text = text)
            Log.d("HistoryRepositoryImpl", "🎯 댓글 등록 성공 - CommentID: $commentId")
            Result.Success(commentId)
        } catch (e: FirebaseFunctionsException) {
            Result.Failure(AppError.Unknown("[댓글 서버 코드 ${e.code}]: ${e.message}"))
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown(e.message ?: "댓글 등록 도중 예기치 못한 에러 발생"))
        }
    }

    // 히스토리 저장
    override suspend fun saveMyHistory(
        category: String,
        placeName: String,
        content: String,
        latitude: Double,
        longitude: Double,
        images: List<ByteArray>
    ): Result<String> {
        return try {
            // 사용자 인증 정보 검증
            val currentUid = authDataSource.getCurrentUserUid()
                ?: return Result.Failure(AppError.Unknown("인증된 사용자 정보가 없습니다."))

            // 이미지 업로드 및 파일 URL 리스트 획득
            val uploadedUrls = if (images.isNotEmpty()) {
                remoteDataSource.uploadHistoryImages(uid = currentUid, images = images)
            } else {
                emptyList()
            }

            val requestDto = HistoryRequestDto(
                category = category,
                placeName = placeName,
                content = content,
                latitude = latitude,
                longitude = longitude,
                imageUrls = uploadedUrls
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
