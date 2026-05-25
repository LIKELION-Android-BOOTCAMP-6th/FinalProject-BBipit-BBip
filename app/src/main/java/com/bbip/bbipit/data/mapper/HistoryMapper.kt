package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.domain.entity.History
import kotlin.collections.get

object HistoryMapper {

    // Cloud Function 통신 응답 Map -> Domain Entity 변환
    fun Map<*, *>.toDomainHistory(): History? {
        return try {
            History(
                id = this["id"] as? String ?: "",
                userId = this["userId"] as? String ?: "",
                userNickname = this["userNickname"] as? String ?: "",
                userProfileImage = this["userProfileImage"] as? String ?: "",
                category = this["category"] as? String ?: "",
                placeName = this["placeName"] as? String ?: "",
                geohash = this["geohash"] as? String ?: "",
                content = this["content"] as? String ?: "",
                imageUrl = this["imageUrl"] as? String ?: "",
                latitude = (this["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (this["longitude"] as? Number)?.toDouble() ?: 0.0,
                createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            null
        }
    }
}