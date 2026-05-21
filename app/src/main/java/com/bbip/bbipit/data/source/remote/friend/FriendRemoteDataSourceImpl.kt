package com.bbip.bbipit.data.source.remote.friend

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRemoteDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: FirebaseFirestore,
    private val function: FirebaseFunctions
    ): FriendRemoteDataSource {
}