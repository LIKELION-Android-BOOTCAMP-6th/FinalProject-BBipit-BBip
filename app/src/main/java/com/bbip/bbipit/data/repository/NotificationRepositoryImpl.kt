package com.bbip.bbipit.data.repository

import android.util.Log
import com.bbip.bbipit.core.result.Result
import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.model.NotificationDto
import com.bbip.bbipit.data.source.remote.notification.NotificationRemoteDataSource
import com.bbip.bbipit.domain.entity.Notification
import com.bbip.bbipit.domain.entity.VoiceMessage
import com.bbip.bbipit.domain.error.AppError
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.bbip.bbipit.domain.repository.VoiceRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 관련 데이터 처리를 담당하는 구현체입니다.
 * 알림 목록 조회, 읽음 처리, 실시간 관찰 기능을 제공합니다.
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dataSource: NotificationRemoteDataSource,
    private val firebaseFunctions: FirebaseFunctions,
    private val voiceRepository: VoiceRepository,
) : NotificationRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    override val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _uiReadIds = MutableStateFlow<Set<String>>(emptySet())
    override val uiReadIds: StateFlow<Set<String>> = _uiReadIds.asStateFlow()

    // Firestore 실시간 리스너 등록 객체 (중복 구독 방지용)
    private var listenerRegistration: ListenerRegistration? = null

    // 현재 구독 중인 userId (중복 호출 방지용)
    private var observingUserId: String? = null

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun markVoiceNotiAsPlayed(notificationId: String): Boolean {
        if (notificationId.isBlank()) return false

        return dataSource.markVoiceNotificationAsPlayed(notificationId)
    }


    /**
     * 앱 수명 스코프로 Firestore 구독 시작 (로그인 직후 1회 호출)
     * 구독 즉시 전체 문서를 수신하여 캐시에 보관
     * 동일한 userId로 이미 구독 중이면 중복 구독 방지
     */
    override fun startObserving(userId: String) {
        // 동일 유저 중복 구독 방지
        if (observingUserId == userId) {
            Log.d("NotificationRepo", "이미 구독 중인 userId: $userId, 중복 호출 무시")
            return
        }

        // 기존 리스너 제거 후 새로 등록
        stopObserving()
        observingUserId = userId

        val query = firestore
            .collection("Notifications")
            .document(userId)
            .collection("Notification")
            .orderBy("created_at", Query.Direction.DESCENDING)

        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("NotificationRepo", "실시간 알림 구독 실패: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                Log.d("NotificationRepo", "Firestore 스냅샷 수신! 변경된 문서 수: ${snapshot.documentChanges.size}"
                )

                // 전체 문서를 엔티티로 변환
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        val dto = doc.toObject(NotificationDto::class.java)
                        val realIsReadFromServer = doc.getBoolean("is_read") ?: false

                        dto?.toEntity(doc.id)?.copy(isRead = realIsReadFromServer)
                    } catch (e: Exception) {
                        Log.e("NotificationRepo", "데이터 변환 실패: ${doc.id}, 에러: ${e.message}")
                        null
                    }
                }

                val mergedItems = items.map { newItem ->
                    val cachedItem = _notifications.value.find { it.id == newItem.id }

                    val finalIsRead = newItem.isRead ||
                            (cachedItem?.isRead == true) ||
                            _uiReadIds.value.contains(newItem.id)

                    newItem.copy(isRead = finalIsRead)
                }

                _notifications.value = mergedItems
                Log.d("NotificationRepo", "🔄 실시간 동기화 완료: ${mergedItems.size}건 갱신됨")
            }
        }
    }

    /**
     * 구독 중단 (로그아웃 시 호출)
     */
    override fun stopObserving() {
        listenerRegistration?.remove()
        listenerRegistration = null
        observingUserId = null
        Log.d("NotificationRepo", "Firestore 알림 구독 중단 및 캐시 초기화")
    }
    // 알림 목록 조회
    override suspend fun getNotificationList(userId: String): Result<List<Notification>> {
        return try {
            val response = dataSource.fetchNotification(userId)
            Result.Success(response.map { (id, dto) -> dto.toEntity(id) })
        } catch (e: Exception) {
            Log.e("NotificationRepository", "알림 목록 조회 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알림 목록을 가져오지 못했습니다."))
        }
    }

    // 알림 읽음 처리
    override suspend fun markNotificationsAsRead(
        type: String,
        notificationId: String?
    ): Result<Boolean> {
        val data = hashMapOf(
            "type" to type,
            "notificationId" to notificationId
        )
        return try {
            val result = firebaseFunctions
                .getHttpsCallable("markNotificationsAsRead")
                .call(data)
                .await()
            val res = result.data as? Map<*, *>
            
            // 읽음 처리 성공 시 UI 상태 반영
            if (notificationId != null) {
                _uiReadIds.value += notificationId
            } else if (type == "all") {
                _notifications.value = _notifications.value.map { it.copy(isRead = true) }
            }
            
            Result.Success(res?.get("success") as? Boolean ?: true)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "알림 읽음 처리 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "알림 읽음 처리 중 오류 발생"))
        }
    }

    // 단건 알림 읽음 처리
    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            dataSource.markAsRead(notificationId)

            val updatedList = _notifications.value.map { notification ->
                if (notification.id == notificationId) notification.copy(isRead = true)
                else notification
            }
            _notifications.value = updatedList

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "단건 읽음 처리 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "읽음 처리 실패"))
        }
    }

    // 실시간 구독
    override suspend fun deleteNotifications(userId: String, id: String?): Result<Unit> {
        if (id != null) {
            // 로컬 캐시에서 즉시 제거
            _notifications.value = _notifications.value.filter { it.id != id }
        } else {
            _notifications.value = emptyList()
        }

        val data = hashMapOf(
            "type" to if (id == null) "all" else "single",
            "notificationId" to id
        )

        return try {
            firebaseFunctions
                .getHttpsCallable("deleteNotifications")
                .call(data)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "서버 삭제 실패: ${e.message}")
            Result.Failure(AppError.Unknown(e.message ?: "삭제 실패"))
        }
    }

    override fun clearCache() {
        _notifications.value = emptyList()
        _uiReadIds.value = emptySet()
        Log.d("NotificationRepo", "캐시 완전 초기화 (로그아웃)")
    }

    // 무전 알림 → VoiceMessage 재생 처리
    override suspend fun playWalkieNotification(notification: Notification, receiverId: String) {
        val voiceMessage = VoiceMessage(
            id = notification.id,
            senderId = notification.senderId,
            receiverId = receiverId,
            voiceUrl = notification.audioUrl,
            duration = notification.duration,
            isRead = false,
            createdAt = notification.createdAt
        )
        voiceRepository.emitMobileVoiceEvent(voiceMessage)
    }

    // 무전 즉시 재생
    override fun playWalkie(intent: android.content.Intent, receiverId: String) {
        val audioId = intent.getStringExtra("notification_audio_id") ?: ""
        val audioUrl = intent.getStringExtra("notification_audio_url") ?: return
        val senderId = intent.getStringExtra("notification_sender_id") ?: ""
        val createdAt = intent.getLongExtra("notification_created_at", 0L)
        val duration = intent.getIntExtra("notification_duration", 0)

        val voiceMessage = VoiceMessage(
            id = audioId,
            senderId = senderId,
            receiverId = receiverId,
            voiceUrl = audioUrl,
            duration = duration,
            isRead = false,
            createdAt = createdAt
        )
        appScope.launch {
            voiceRepository.emitMobileVoiceEvent(voiceMessage)
        }
    }
}