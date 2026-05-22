package com.bbip.bbipit.domain.type

import com.bbip.bbipit.R

enum class LoginType(val type: String, val img: Int?, val provider: String) {
    KAKAO("KAKAO", R.drawable.ic_signin_kakao, "oidc.kakao"),
    GOOGLE("GOOGLE", R.drawable.ic_signin_google, "google.com"),
    EMAIL("EMAIL", null, "password")
}