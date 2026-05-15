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

// 검색된 유저 정보를 담을 간단한 데이터 모델 (나중에 domain 레이어로 이동 가능)
data class SearchedUser(
    val uid: String,
    val name: String,
    val profileImageUrl: String? = null
)

@HiltViewModel
class MyPageViewModel @Inject constructor() : ViewModel() {

    var isAddFriendDialogShowing by mutableStateOf(false)
        private set

    // 사용자가 입력하는 UID 쿼리 상태
    var searchQuery by mutableStateOf("")
        private set

    // 검색된 유저 결과 상태 (null이면 결과 없음)
    var searchedUser by mutableStateOf<SearchedUser?>(null)
        private set

    // 현재 로그인한 사용자의 프로필 상태
    var userProfile by mutableStateOf(
        UserProfile(
            name = "yukong", // 유빈님 이름으로 초기값 세팅!
            uid = "9823-4812",
            deviceModel = "Galaxy A33"
        )
    )
        private set

    // 나중에 Firestore에서 데이터를 가져오면 이 함수를 호출
    fun loadUserProfile() {
        // userProfile = repository.getUserData() ...
    }

    // 입력값 변경 함수
    fun onQueryChange(newQuery: String) {
        searchQuery = newQuery
    }

    // 친구 추가 버튼 클릭 로그 (기존 유지)
    fun onAddFriendClicked() {
        Log.d("MyPageViewModel", "친구 추가 버튼이 클릭되었습니다!")
        isAddFriendDialogShowing = true
    }

    // 실제 친구 검색 로직
    fun searchFriend() {
        if (searchQuery.isBlank()) { // 공백일 경우
            Log.d("AddFriend", "UID가 비어있습니다.")
            return
        }

        Log.d("AddFriend", "검색 시작! 입력된 UID: $searchQuery")

        // TODO: 나중에 이 부분에 Firestore 검색 로직을 넣으세요.
        // 지금은 테스트용으로 더미 데이터를 넣어 결과창이 뜨는지 확인
        if (searchQuery == "9823-4812") {
            searchedUser = SearchedUser(
                uid = "9823-4812",
                name = "Yubin Lee" // 테스트 데이터
            )
        } else {
            searchedUser = null
            Log.d("AddFriend", "해당 UID의 유저를 찾을 수 없습니다.")
        }
    }

    // 다이얼로그 닫힐 때 상태 초기화
    fun clearSearchState() {
        searchQuery = ""
        searchedUser = null
    }

    fun dismissDialog() {
        isAddFriendDialogShowing = false
        clearSearchState() // 닫을 때 데이터 초기화까지
    }
}