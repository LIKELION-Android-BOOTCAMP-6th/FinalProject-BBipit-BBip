package com.bbip.bbipit.data.source.remote.history

import android.util.Log
import com.bbip.bbipit.data.mapper.toDomainHistory
import com.google.firebase.firestore.Query
import com.bbip.bbipit.domain.entity.HistoryComment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.bbip.bbipit.data.source.model.HistoryRequestDto
import com.bbip.bbipit.domain.entity.History
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRemoteDataSourceImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore,
): HistoryRemoteDataSource {

    override fun observeHistoriesByUidList(uidList: List<String>): Flow<List<History>> = callbackFlow {
        // 빈 리스트가 들어오면 바로 종료하여 whereIn 쿼리 크래시 방지
        if (uidList.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = firestore.collection("History")
            .whereIn("userId", uidList.take(30))
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("HistoryRemoteDataSource", "❌ 히스토리 관측 에러 발생: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val histories = snapshot.documents.mapNotNull { doc ->
                    try {
                        val map = doc.data?.toMutableMap() ?: mutableMapOf<String, Any>()
                        if (map["id"] == null || (map["id"] as? String).isNullOrEmpty()) {
                            map["id"] = doc.id
                        }
                        map.toDomainHistory()
                    } catch (e: Exception) {
                        Log.e("HistoryRemoteDataSource", "데이터 매핑 실패 (사진 URL 등 필드 타입 확인 필요): ${e.message}")
                        null // 특정 문서 파싱 실패 시 리스너 전체가 죽지 않도록 null 반환 처리
                    }
                }
                trySend(histories)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    override fun observeHistoryComments(historyId: String): Flow<List<HistoryComment>> = callbackFlow {
        val query = firestore.collection("History")
            .document(historyId)
            .collection("Comments")
            .orderBy("createdAt", Query.Direction.ASCENDING) // 오름차순(과거 -> 최신) 정렬

        // 실시간 스냅샷 리스너 부착
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error) // 에러 발생 시 Flow 스트림 종료
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val comments = snapshot.documents.mapNotNull { doc ->
                    try {
                        HistoryComment(
                            id = doc.id,
                            userId = doc.getString("userId").orEmpty(),
                            userNickname = doc.getString("userNickname").orEmpty(),
                            userProfileImage = doc.getString("userProfileImage").orEmpty(),
                            text = doc.getString("text").orEmpty(),
                            // 서버 타임스탬프 공백 시 현재 시스템 타임으로 방어
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(comments)
            }
        }

        // Flow 수집(Collect)이 중단되거나 화면이 파기될 때 리스너를 자동으로 해제하여 메모리 누수 원천 차단
        awaitClose { listenerRegistration.remove() }
    }

    // 히스토리 코멘트 추가
    override suspend fun addHistoryComment(historyId: String, text: String): String {
        val data = hashMapOf(
            "historyId" to historyId,
            "text" to text
        )

        val result = functions
            .getHttpsCallable("addHistoryComment")
            .call(data)
            .await()

        val responseData = result.data as? Map<*, *>
        return responseData?.get("commentId") as? String
            ?: throw IllegalStateException("서버가 생성된 댓글 ID를 반환하지 않았습니다.")
    }

    // 히스토리 사진 저장
    override suspend fun uploadHistoryImages(uid: String, images: List<ByteArray>): List<String> {
        if (images.isEmpty()) return emptyList()

        val downloadUrls = mutableListOf<String>()

        // 추후 회원탈퇴 시 'history/{uid}' 폴더 전체를 지울 수 있도록 경로 격리
        // 파일명 중복 방지 및 순서 유지를 위해 UUID_index 조합 사용
        val batchGroupId = UUID.randomUUID().toString()

        images.forEachIndexed { index, byteArray ->
            val fileName = "${batchGroupId}_$index.jpg"
            val storageRef = storage.reference.child("history/$uid/$fileName")

            // 업로드 작업 수행 후 await()로 대기
            storageRef.putBytes(byteArray).await()

            // 업로드 완료된 파일의 public 다운로드 URL 추출
            val downloadUrl = storageRef.downloadUrl.await().toString()
            downloadUrls.add(downloadUrl)
        }

        return downloadUrls
    }

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