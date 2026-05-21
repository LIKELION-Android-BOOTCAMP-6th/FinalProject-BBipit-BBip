package com.bbip.bbipit.data.repository

import com.bbip.bbipit.data.source.remote.friend.FriendRemoteDataSource
import com.bbip.bbipit.data.source.remote.user.UserRemoteDataSource
import com.bbip.bbipit.domain.repository.FriendRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 친구 관련 데이터 처리


 */
@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val friendRemoteDataSource: FriendRemoteDataSource
): FriendRepository {


}