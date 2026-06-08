package com.bbip.bbipit.core.util

object HangulUtils {
    private val CHOSUNG_LIST = charArrayOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    )

    // 글자 하나를 받아 초성을 추출
    fun getChosung(char: Char): Char {
        return if (char.toInt() in 0xAC00..0xD7A3) {
            CHOSUNG_LIST[(char.toInt() - 0xAC00) / 588]
        } else {
            char // 한글이 아니면 그대로 반환
        }
    }

    // 문자열 전체를 초성으로 변환 (예: "김철수" -> "ㄱㅊㅅ")
    fun getChosungString(str: String): String {
        return str.map { getChosung(it) }.joinToString("")
    }
}