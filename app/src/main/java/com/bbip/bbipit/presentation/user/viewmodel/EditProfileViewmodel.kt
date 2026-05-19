package com.bbip.bbipit.presentation.mypage

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.presentation.base.UserStatusType
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // 저장 성공 여부를 스크린에 알림
    private val _saveSuccessEvent = MutableSharedFlow<Boolean>()
    val saveSuccessEvent: SharedFlow<Boolean> = _saveSuccessEvent.asSharedFlow()

    // 마이페이지에서 들고 있던 기존 내 계정 정보를 전달받아 초기화
    fun initWithUserData(currentNickname: String, currentStatusMessage: String, photoUrl: String) {
        _uiState.update {
            it.copy(
                nickname = currentNickname,
                statusMessage = currentStatusMessage,
                profileImageUrl = photoUrl
            )
        }
    }

    // 닉네임 입력 업데이트
    fun updateNickname(newNickname: String) {
        _uiState.update { it.copy(nickname = newNickname, isNicknameError = newNickname.isBlank()) }
    }

    // 상태 메시지 선택 업데이트
    fun updateStatusMessage(newStatus: String) {
        _uiState.update { it.copy(statusMessage = newStatus) }
    }

    // 바텀시트 표시 여부 제어
    fun setBottomSheetVisibility(isVisible: Boolean) {
        _uiState.update { it.copy(isBottomSheetVisible = isVisible) }
    }

    /**
     * 서버의 updateProfile API를 호출하여 수정사항 반영
     */
    fun saveProfileChanges(imageUri: Uri?) {
        viewModelScope.launch {
            // 이미지 변경되었으면 firestore storage 업로드
            val photoUrl = if (imageUri != null) {
                try {
                    println("DEBUG: 업로드 시작...")
                    val url = uploadImageToStorage(imageUri)
                    println("DEBUG: 업로드 성공! URL: $url") // 2. 업로드 성공 확인
                    url
                } catch (e: Exception) {
                    println("DEBUG: 업로드 실패: ${e.message}") // 3. 업로드 에러 확인
                    null
                }
            } else {
                println("DEBUG: 이미지 선택 안 함, 기존 이미지 유지")
                null
            }

            val data = hashMapOf(
                "nickname" to _uiState.value.nickname,
                "statusMessage" to _uiState.value.statusMessage
                // 필요 시 추후 photoURL 등도 이곳에 추가 가능합니다.
            )

            if (photoUrl != null) {
                data["photoURL"] = photoUrl
                println("DEBUG: 서버 전송 데이터: $data") // 4. 서버로 가는 데이터 확인
            }

            try {
                // Cloud Functions의 "updateProfile" 호출
                val result = functions
                    .getHttpsCallable("updateProfile")
                    .call(data)
                    .await()
                println("DEBUG: 서버 응답 성공: ${result.data}") // 5. 응답 확인

                // 서버에서 return { success: true }; 가 정상적으로 왔는지 확인
                if (result.data is Map<*, *> && (result.data as Map<*, *>)["success"] == true) {
                    _saveSuccessEvent.emit(true) // 성공 신호 송출
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is java.net.UnknownHostException -> "네트워크 연결을 확인해주세요."
                    else -> "오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                }
                sendToast(errorMessage)
                _saveSuccessEvent.emit(false)

            }
        }
    }
    private suspend fun uploadImageToStorage(uri: Uri): String {
        val fileName = "profile_${UUID.randomUUID()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$fileName")

        // 파일 업로드
        storageRef.putFile(uri).await()

        // 업로드된 파일의 다운로드 URL 가져오기
        return storageRef.downloadUrl.await().toString()
    }

    // 에러 발생 시 호출
    private fun sendToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(message)
        }
    }
}