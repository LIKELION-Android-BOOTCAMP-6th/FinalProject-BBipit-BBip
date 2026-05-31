package com.bbip.bbipit.models

/**
 * 워치가 감지한 휴대폰 앱의 백그라운드 서비스 상태
 */
enum class MobileServiceStatus {
    CHECKING,          // 초기 상태: 휴대폰 상태를 확인 중
    READY,             // 정상: 휴대폰 백그라운드가 잘 돌고 있음 (지도 화면 표시)
    SERVICE_RESTRICTED // 불능: 휴대폰 서비스 이용 불가 (제한 화면 표시)
}