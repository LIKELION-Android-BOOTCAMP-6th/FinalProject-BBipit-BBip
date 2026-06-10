package com.bbip.bbipit.data.source.model

data class HistoryDto(
    val category: String,
    val placeName: String,
    val content: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrls: List<String>,
) {
    // 서버 전송용 Map 객체 변환
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "category" to category,
            "placeName" to placeName,
            "content" to content,
            "latitude" to latitude,
            "longitude" to longitude,
            "imageUrls" to imageUrls
        )
    }
}