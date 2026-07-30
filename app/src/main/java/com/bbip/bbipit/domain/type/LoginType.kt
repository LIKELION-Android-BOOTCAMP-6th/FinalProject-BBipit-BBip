package com.bbip.bbipit.domain.type

import com.bbip.bbipit.R

enum class LoginType(val type: String, val img: Int?, val provider: String) {
    KAKAO("KAKAO", R.drawable.ic_signin_kakao, "oidc.kakao"),
    GOOGLE("GOOGLE", R.drawable.ic_signin_google, "google.com"),
    EMAIL("EMAIL", null, "password");

    companion object {
        fun fromString(value: String?): LoginType {
            return entries.find { it.type.equals(value, ignoreCase = true) } ?: EMAIL
        }
    }
}