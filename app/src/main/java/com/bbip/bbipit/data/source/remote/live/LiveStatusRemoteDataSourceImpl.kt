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
 * 파이어스토어 컬렉션 및 파이어베이스 클라우드 함수 인스턴스 활용 실시간 수명 주기 처리 데이터 소스 구현체 클래스
 */
@Singleton
class LiveStatusRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : LiveStatusRemoteDataSource {

    /**
     * 사용자 실시간 온라인 활성화 신호 및 현재 진입 채팅방 식별 정보 원격 백엔드 전송 함수
     * 파이어베이스 클라우드 함수 내 사전 빌드 호출 객체 트리거를 통한 수명 주기 세션 비동기 동기화 목적
     */
    override suspend fun updateLifeCycle(currentRoomId: String?) {
        val data = hashMapOf("isOnline" to true, "currentRoomId" to currentRoomId)
        functions.getHttpsCallable("updateUserStatus").call(data).await()
    }

    /**
     * 내 실시간 위경도 좌표 및 접속 프로필 맵 데이터의 파이어스토어 Live 컬렉션 영역 강제 오버라이트 적재 함수
     * 파이어베이스 SDK 고유 영속성 로컬 오프라인 캐시 보관 메커니즘 지원 목적 (별도 스레드 대기 없음)
     */
    override fun updateMyLiveStatus(uid: String, dto: LiveStatusDto) {
        firestore.collection("Live")
            .document(uid)
            .set(dto.toMap())
    }

    /**
     * 타인 상태 변화 내역 파이어스토어 실시간 이벤트 리스너 기준 연속 캐치 및 스트림 데이터 통로 표출 함수
     * 물리 네트워크 단절 시 내부 로컬 캐시 스냅샷 선제 로드 및 캐시 적재 플래그 쌍 결합 동기화 상시 전달 목적
     * 흐름 전면 종료 및 소멸 시점 실시간 이벤트 리스너 제거 처리를 통한 메모리 손실 원천 예방
     */
    override fun observeUserLiveStatus(uid: String): Flow<Pair<LiveStatusDto, Boolean>> = callbackFlow {
        val listenerRegistration = firestore.collection("Live")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val dto = snapshot.data.toDto()
                    val isFromCache = snapshot.metadata.isFromCache
                    trySend(Pair(dto, isFromCache))
                } else {
                    trySend(Pair(LiveStatusDto(), false))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    /**
     * 특정 사용자 UID 코드 조건 기준 클라우드 함수 게이트웨이 경유 실시간 라이브 수명 주기 DTO 세트 단발성 직접 호출 조회 함수
     * 반환 결과 트리 구조 데이터 내 물리 맵 스키마 분해 파싱 및 확장 변환 매퍼 코드를 통한 최종 반환 가공 목적
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