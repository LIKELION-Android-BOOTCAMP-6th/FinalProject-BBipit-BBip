package com.bbip.bbipit.data.mapper

import com.bbip.bbipit.domain.entity.History
import kotlin.collections.get

// Map -> Domain Entity 변환
fun Map<*, *>.toDomainHistory(): History? {

    val rawImageUrls = this["imageUrls"] as? List<*>
    val mappedImageUrls = rawImageUrls?.mapNotNull { it as? String } ?: emptyList()
    return try {
        History(
            id = this["id"] as? String ?: "",
            userId = this["userId"] as? String ?: "",
            userNickname = this["userNickname"] as? String ?: "",
            userProfileImage = this["userProfileImage"] as? String ?: "",
            category = this["category"] as? String ?: "",
            placeName = this["placeName"] as? String ?: "",
            content = this["content"] as? String ?: "",
            latitude = (this["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (this["longitude"] as? Number)?.toDouble() ?: 0.0,
            createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
            imageUrls = mappedImageUrls
        )
    } catch (e: Exception) {
        null
    }
}