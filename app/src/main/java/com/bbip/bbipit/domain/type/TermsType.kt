package com.bbip.bbipit.domain.type

enum class TermsType(val url: String) {
    SERVICE("https://raw.githubusercontent.com/LIKELION-Android-BOOTCAMP-6th/FinalProject-BBipit-BBip/refs/heads/release/v1/docs/service.md"),  // 서비스 이용약관
    PRIVACY("https://raw.githubusercontent.com/LIKELION-Android-BOOTCAMP-6th/FinalProject-BBipit-BBip/refs/heads/release/v1/docs/terms.md"), // 개인정보 처리방침
    LOCATION("https://raw.githubusercontent.com/LIKELION-Android-BOOTCAMP-6th/FinalProject-BBipit-BBip/refs/heads/release/v1/docs/location.md")
}