package com.bbip.bbipit.presentation.mypage

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


// 프로필 정보를 담을 데이터 모델
data class UserProfile(
    val name: String,
    val uid: String,
    val deviceModel: String,
    val profileImageUrl: String? = null
)

@HiltViewModel
class MyPageViewModel @Inject constructor() : ViewModel() {

    // 현재 로그인한 사용자의 프로필 상태
    var userProfile by mutableStateOf(
        UserProfile(
            name = "yukong", // 유빈님 이름으로 초기값 세팅!
            uid = "9823-4812",
            deviceModel = "Galaxy A33"
        )
    )
        private set
}