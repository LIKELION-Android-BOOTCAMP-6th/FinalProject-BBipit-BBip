package com.bbip.bbipit.data.datasource

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRemoteDataSource{
}