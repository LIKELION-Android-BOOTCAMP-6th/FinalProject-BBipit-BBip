package com.bbip.bbipit.data.source.remote.live

import com.bbip.bbipit.data.mapper.toDto
import com.bbip.bbipit.data.source.model.LiveStatusDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 실시간 상태 및 위치 정보 관련 원격 데이터 소스 구현체
 */
@Singleton
class LiveStatusRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : LiveStatusRemoteDataSource {

    /**
     * 유저 온라인 상태 및 현재 채팅방 정보 업데이트 함수
     */
    override suspend fun updateLifeCycle(currentRoomId: String?) {
        val data = hashMapOf("isOnline" to true, "currentRoomId" to currentRoomId)
        functions.getHttpsCallable("updateUserStatus").call(data).await()
    }

    /**
     * Live 컬렉션에 내 실시간 위치를 업데이트하는 함수
     */
    override fun updateMyLiveStatus(uid: String, dto: LiveStatusDto) {
        firestore.collection("Live")
            .document(uid)
            .set(dto.toMap())
    }

    /**
     * 다른 유저의 실시간 상태 변화를 구독(관찰)하는 Flow 생성 함수
     */
    override fun observeUserLiveStatus(uid: String): Flow<Pair<LiveStatusDto, Boolean>> = callbackFlow {
        val listenerRegistration = firestore.collection("Live")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // 캐시 데이터 사용 여부와 상태 DTO 추출
                if (snapshot != null && snapshot.exists()) {
                    val dto = snapshot.data.toDto()
                    val isFromCache = snapshot.metadata.isFromCache
                    trySend(Pair(dto, isFromCache))
                } else {
                    trySend(Pair(LiveStatusDto(), false))
                }
            }

        // 구독 해제 시 리스너 제거
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * 특정 유저의 실시간 상태 정보를 1회성으로 조회하는 함수
     */
    override suspend fun getLiveStatusByUid(targetUid: String): LiveStatusDto {
        val data = mapOf("targetUid" to targetUid)

        val result = functions
            .getHttpsCallable("getLiveStatusByUid")
            .call(data)
            .await()

        val resultMap = result.data as? Map<String, Any>
        val liveMap = resultMap?.get("live") as? Map<String, Any>

        return liveMap.toDto()
    }
}