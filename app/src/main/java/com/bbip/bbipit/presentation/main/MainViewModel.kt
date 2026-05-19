/*
package com.bbip.bbipit.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.entity.LiveStatus
import com.bbip.bbipit.domain.repository.LiveStatusRepository
import com.bbip.bbipit.domain.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    // 💡 1. 전역 라이브 상태 제어를 위해 리포지토리를 주입받습니다.
    private val liveStatusRepository: LiveStatusRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private var isSessionStarted = false // 중복 실행 방지용 플래그

    val myLiveStatus: StateFlow<LiveStatus?> = liveStatusRepository.myLiveStatusFlow

    // 💡 FirebaseAuth의 상태 리스너 정의
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid

        if (uid != null) {
            // 딜레이 후 로그인이 완료되었거나, 새로 로그인에 성공한 경우 모두 이곳으로 들어옵니다.
            if (!isSessionStarted) {
                Log.d("MainViewModel", "로그인 감지 성공! 전역 라이브 세션을 시작합니다. UID: $uid")
                startLiveSession(uid)
                startNotificationCaching(uid)
                isSessionStarted = true
            }
        } else {
            // 로그아웃 되었거나 로그인 정보가 없는 경우
            Log.d("MainViewModel", "로그인된 유저가 없습니다.")
            isSessionStarted = false
        }
    }

    init {
        // 💡 뷰모델이 켜지자마자 리스너를 Firebase 시스템에 등록합니다.
        auth.addAuthStateListener(authStateListener)
    }

    private fun startNotificationCaching(uid: String) {
        viewModelScope.launch {
            try {
                // 이 수집(Collect) 행위 자체가 리포지토리의 StateFlow 캐시를 상시 살아있게 유지해 줍니다.
                notificationRepository.observeNotification(uid).collectLatest { cachedList ->
                    Log.d("MainViewModel", "🔔 [전역 캐시 동기화] 보관된 알림: ${cachedList.size}개")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ 전역 알림 캐싱 실패: ${e.message}")
            }
        }
    }

    fun startLiveSession(uid: String) {
        // 이 공간에 서버에서 데이터를 딱 한 번 불러오는 로직이 들어갈 예정입니다.
        viewModelScope.launch {
            try {
                val result = liveStatusRepository.getLiveStatusByUid(uid)

                result.onSuccess { savedStatus ->
                    val initData = savedStatus.copy(
                        isOnline = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    // 동기화 시작
                    liveStatusRepository.updateMyLiveStatus(initData)

                    Log.d("MainViewModel", "이제부터 라이브 데이터 동기화를시작합니다!")
                }.onFailure {
                    // 서버에 아예 Live 도큐먼트가 없는 경우 등의 예외 처리
                    val newData = LiveStatus(uid = uid, isOnline = true)
                    liveStatusRepository.updateMyLiveStatus(newData)
                }
            } catch (e: Exception){
                Log.e("MainViewModel", "라이브 데이터 동기화 실패했습니다!: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 💡 뷰모델 파괴 시 메모리 누수 방지를 위해 리스너를 안전하게 제거합니다.
        auth.removeAuthStateListener(authStateListener)
    }
}*/
