package com.bbip.bbipit.data.repository

import com.bbip.bbipit.data.mapper.toEntity
import com.bbip.bbipit.data.source.remote.noti.NotiRemoteDataSource
import com.bbip.bbipit.domain.entity.NotiItem
import com.bbip.bbipit.domain.repository.NotiRepository
import javax.inject.Inject

class NotiRepositoryImpl @Inject constructor(
    private val dataSource: NotiRemoteDataSource
) : NotiRepository {

    override suspend fun getNotiList(userId: String): Result<List<NotiItem>> {
        return try {
            val response = dataSource.fetchNotifications(userId)
            Result.success(response.map { (id, dto) -> dto.toEntity(id) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}