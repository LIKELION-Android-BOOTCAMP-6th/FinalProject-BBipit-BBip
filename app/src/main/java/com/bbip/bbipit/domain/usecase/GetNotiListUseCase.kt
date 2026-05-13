package com.bbip.bbipit.domain.usecase

import com.bbip.bbipit.domain.entity.NotiItem
import com.bbip.bbipit.domain.repository.NotiRepository
import javax.inject.Inject

class GetNotiListUseCase @Inject constructor(
    private val repository: NotiRepository
) {
    suspend operator fun invoke(userId: String): Result<List<NotiItem>> {
        return repository.getNotiList(userId)
    }
}