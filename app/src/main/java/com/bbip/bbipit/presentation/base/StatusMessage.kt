package com.bbip.bbipit.presentation.base

/**
 * 유저의 현재 무전 및 활동 상태를 정의하는 공통 Enum 클래스
 * * @property text 화면에 표시될 한글 문구 및 이모지
 */
enum class UserStatusType(val text: String) {
    AVAILABLE("지금 무전 가능! 🎙️"),
    DRIVING("운전 중이에요 🚗"),
    WORKING("업무 중... 💻"),
    SLEEPING("잠자는 중 💤"),
    EXERCISING("운동 중입니다 🏃"),
    BUSY("대화하기 어려워요 🔕");

    companion object {
        // 초기 기본값 상수화
        val DEFAULT_STATUS = AVAILABLE.text

        // 바텀시트 리스트에 뿌려줄 모든 텍스트 목록 추출 확장 프로퍼티
        val allTexts: List<String> = entries.map { it.text }
    }
}