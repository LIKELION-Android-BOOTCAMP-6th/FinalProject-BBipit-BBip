package com.bbip.bbipit.domain.type

import com.bbip.bbipit.R

enum class LoginType(val type: String, val img: Int?) {
    KAKAO("KAKAO", R.drawable.ic_signin_kakao),
    GOOGLE("GOOGLE", R.drawable.ic_signin_google),
    EMAIL("EMAIL", null)
}