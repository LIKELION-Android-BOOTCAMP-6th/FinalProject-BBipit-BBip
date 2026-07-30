package com.bbip.bbipit.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

/**
 * ADB 명령어로 테스트 알림을 생성하기 위한 브로드캐스트 리시버 (디버그 전용)
 * 사용법: adb shell am broadcast -a com.bbip.bbipit.TEST_NOTIFICATION --es type REQ --es userId {userId}
 */
class TestNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: "REQ"
        // userId를 ADB extras로 직접 받음
        val userId = intent.getStringExtra("userId") ?: run {
            Log.e("TestReceiver", "❌ userId 없음 — --es userId {your_uid} 를 추가하세요")
            return
        }

        Log.d("TestReceiver", "✅ userId: $userId, type: $type")

        val generatedId = FirebaseFirestore.getInstance()
            .collection("Notifications")
            .document(userId)
            .collection("Notification")
            .document().id

        val testData = hashMapOf(
            "type" to type,
            "sender_name" to when (type) {
                "DM" -> "홍길동(DM)"
                "WALKIE" -> "김철수(무전)"
                else -> "이영희(친구요청)"
            },
            "content" to when (type) {
                "DM" -> "지금 뭐해? 메시지 보냄!"
                "WALKIE" -> "치익- 무전을 보냈습니다."
                else -> "친구 요청을 보냈습니다."
            },
            "is_read" to false,
            "created_at" to Timestamp.now(),
            "room_id" to if (type == "DM") "test_room_123" else null,
            "expires_at" to if (type == "WALKIE") Timestamp(
                Date(System.currentTimeMillis() + (3 * 60 * 60 * 1000L))
            ) else null
        )

        FirebaseFirestore.getInstance()
            .collection("Notifications")
            .document(userId)
            .collection("Notification")
            .document(generatedId)
            .set(testData)
            .addOnSuccessListener {
                Log.d("TestReceiver", "✅ 테스트 알림 생성 성공: $type")
            }
            .addOnFailureListener { e ->
                Log.e("TestReceiver", "❌ 테스트 알림 생성 실패: ${e.message}")
            }
    }
}