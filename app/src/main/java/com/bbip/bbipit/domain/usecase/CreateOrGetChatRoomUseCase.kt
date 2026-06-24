package com.bbip.bbipit.domain.usecase

import com.bbip.bbipit.core.result.onFailure
import com.bbip.bbipit.core.result.onSuccess
import com.bbip.bbipit.domain.repository.ChatRepository // 인터페이스를 바라보도록 설정
import javax.inject.Inject

/**
 * 특정 사용자와의 1:1 채팅방을 생성하거나 기존 방 ID를 조회하는 유즈케이스
 */
class CreateOrGetChatRoomUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        targetUid: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        chatRepository.createOrGetChatRoom(targetUid)
            .onSuccess { result ->
                if (result.success && result.roomId != null) {
                    onSuccess(result.roomId)
                } else {
                    onError(result.message.ifEmpty { "채팅방 ID를 가져올 수 없습니다." })
                }
            }
            .onFailure { error ->
                onError(error.message ?: "채팅방 생성 중 오류가 발생했습니다.")
            }
    }
}