package com.bbip.bbipit.data.source.remote.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 진짜 구현하는 곳

 */
@Singleton
class AuthRemoteDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) : AuthRemoteDataSource {

    override suspend fun loginWithKakao(): String {
        TODO("Not yet implemented")
    }

    override suspend fun loginWithGoogle(idToken: String) {
        /*
        TODO : 토큰 값 활용
         구글 계정 다이얼로그가 닫히면서 토큰 값이 나오는데
         그걸로 후처리를 해야 하는 것으로 알고 있음
         자세한 것은 더 알아봐야 함
         */
    }

    override suspend fun signInWithCustomToken(accessToken: String) {
        /*
        Todo : 카카오 로그인 구현 시
        로그인 후 반환되는 토큰값이 있는데 그걸 활용해서 어스에 등록시켜 줘야 함
        그래야 카카오 로그인하는 유저도 파이어베이스 어스를 활용해서 컨트롤 할 수 있음

        구글도 이런 식으로 처리해줘야 하는지는 확인 필요
         */
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email,password).await()
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        TODO("Not yet implemented")
    }
}