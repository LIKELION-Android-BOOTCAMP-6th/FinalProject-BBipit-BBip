package com.bbip.bbipit.core.util

import com.google.firebase.Timestamp

object TimeFormatter {

    fun formatTimeAgo(timestamp: Timestamp): String {
        return try {


            val now = System.currentTimeMillis()
            val diff = now - timestamp.toDate().time

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                seconds < 60 -> "방금 전"
                minutes < 60 -> "${minutes}분 전"
                hours < 24 -> "${hours}시간 전"
                days < 7 -> "${days}일 전"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}