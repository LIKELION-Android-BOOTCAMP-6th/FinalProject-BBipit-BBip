package com.bbip.bbipit.data.source.remote.notification

import android.util.Log
import com.bbip.bbipit.data.source.model.NotificationDto
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 파이어스토어 데이터베이스 경유 앱 내부 알림 데이터 조회, 삭제 및 실시간 모니터링 전담 원격 데이터 소스 구현체 클래스
 */
@Singleton
class NotificationRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : NotificationRemoteDataSource {

    override suspend fun markVoiceNotificationAsPlayed(notificationId: String): Boolean {
        val data = hashMapOf(
            "notificationId" to notificationId
        )

        return try {
            val result = functions
                .getHttpsCallable("markVoiceNotificationAsPlayed")
                .call(data)
                .await()

            val resultData = result.data as? Map<*, *>
            resultData?.get("success") as? Boolean ?: false
        } catch (e: Exception) {
            if (e is FirebaseFunctionsException) {
                Log.e("NotificationDS", "서버 에러 [${e.code}]: ${e.message}")
            } else {
                Log.e("NotificationDS", "통신 중 알 수 없는 에러: ${e.localizedMessage}")
            }
            false
        }
    }

    /**
     * 특정 유저 식별자 하위 알림 서브 컬렉션 내 전체 알림 목록 단발성 인출 함수
     * 생성 시간 역순 정렬 조회 및 가용 물리 문서 ID와 DTO 객체 쌍 매핑 반환 목적
     */
    override suspend fun fetchNotification(userId: String): List<Pair<String, NotificationDto>> {
        return firestore
            .collection("Notifications")
            .document(userId)
            .collection("Notification")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val dto = doc.toObject(NotificationDto::class.java) ?: return@mapNotNull null
                Pair(doc.id, dto)
            }
    }

    /**
     * 미독 상태 신규 알림 트래픽 실시간 감시 추적 리스너 등록 함수
     * 문서 추가 이벤트 발생 시 전달받은 외부 콜백 고차 함수 가동을 통한 상위 계층 알림 스냅샷 패킷 바이패스 중계 목적
     */
    override fun observeNotification(
        userId: String,
        onNew: (String, NotificationDto) -> Unit
    ): ListenerRegistration {
        return firestore
            .collection("Notifications")
            .document(userId)
            .collection("Notification")
            .whereEqualTo("is_read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val dto = change.document.toObject(NotificationDto::class.java) ?: return@forEach
                        onNew(change.document.id, dto)
                    }
                }
            }
    }

    /**
     * 지정 특정 알림 고유 문서 식별자 데이터 대상 파이어스토어 데이터베이스 상 영구 삭제 제거 함수
     */
    override suspend fun deleteNotification(userId: String, id: String?) {
        if (id != null) {
            firestore
                .collection("Notifications")
                .document(userId)
                .collection("Notification")
                .document(id)
                .delete()
                .await()
        }
    }
}