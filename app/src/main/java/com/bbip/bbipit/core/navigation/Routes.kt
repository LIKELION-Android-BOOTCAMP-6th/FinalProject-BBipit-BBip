package com.bbip.bbipit.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes {
    @Serializable data object SignIn : Routes
    @Serializable data object SignUp : Routes
    @Serializable data object Map : Routes
    @Serializable data object AuthNotification : Routes
    @Serializable data object ChatList : Routes
    @Serializable data class ChatRoom(val roomId: String) : Routes
    @Serializable data object MyPage : Routes
    @Serializable data object Notification : Routes
    @Serializable data object Setting : Routes

}