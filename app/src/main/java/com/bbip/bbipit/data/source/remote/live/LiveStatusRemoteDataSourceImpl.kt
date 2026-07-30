package com.bbip.bbipit.data.source.remote.live

import android.util.Log
import com.bbip.bbipit.data.mapper.toDto
import com.bbip.bbipit.data.source.model.LiveStatusDto
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
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

    private val TAG = "LiveStatusRemoteDataSourceImpl"
    private val rtdb = FirebaseDatabase.getInstance("https://bbipit-default-rtdb.asia-southeast1.firebasedatabase.app/")

    override fun purgeOutstandingWrites() {
        rtdb.purgeOutstandingWrites()
    }

    override fun setRtdbOnline(uid: String) {
        val userStatusRef = rtdb.getReference("/status/$uid")
        val onlineStatus = mapOf("state" to "online", "last_changed" to ServerValue.TIMESTAMP)
        val offlineStatus = mapOf("state" to "offline", "last_changed" to ServerValue.TIMESTAMP)

        // 온디스커넥트 예약 후 즉시 온라인 상태 설정
        userStatusRef.onDisconnect().setValue(offlineStatus).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                userStatusRef.setValue(onlineStatus)
                Log.d(TAG, "🟢 RTDB 유저 상태 'online' 전환 완료")
            }
        }
    }

    override fun goOnlineRtdb() {
        rtdb.goOnline()
        Log.d(TAG, "⚡ RTDB goOnline() 소켓 개방")
    }

    override fun setRtdbOffline() {
        // 소켓 연결 해제
        rtdb.goOffline()
        Log.d(TAG, "⚡ RTDB goOffline() 소켓 끊기")
    }

    override fun observeRtdbConnection(uid: String): Flow<Boolean> = callbackFlow {
        val connectedRef = rtdb.getReference(".info/connected")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isConnected = snapshot.getValue(Boolean::class.java) ?: false
                trySend(isConnected)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        connectedRef.addValueEventListener(listener)
        awaitClose { connectedRef.removeEventListener(listener) }
    }

    /**
     * 유저 온라인 상태 및 현재 채팅방 정보 업데이트 함수
     */
    override suspend fun updateLifeCycle(currentRoomId: String?) {
        val data = hashMapOf("currentRoomId" to currentRoomId)
        functions.getHttpsCallable("updateHeartbeat").call(data).await()
    }

    /**
     * Live 컬렉션에 내 실시간 위치를 업데이트하는 함수
     */
    override fun updateMyLiveLocation(uid: String, latitude: Double, longitude: Double) {
        val data = mapOf(
            "latitude" to latitude,
            "longitude" to longitude
        )
        firestore.collection("Live")
            .document(uid)
            .update(data)
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