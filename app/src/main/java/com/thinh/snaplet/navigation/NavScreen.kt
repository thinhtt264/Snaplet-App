package com.thinh.snaplet.navigation

import kotlinx.serialization.Serializable

@Serializable
object AuthGraph

@Serializable
object HomeGraph

@Serializable
object Onboarding

@Serializable
object Home

@Serializable
object MyProfile

@Serializable
data class ImageCrop(val sourceUri: String)

@Serializable
data class SpotlightPost(val postId: String)

@Serializable
object ConversationList

@Serializable
data class ChatConversation(
    val conversationId: String? = null,
    val recipientId: String? = null,
    val partnerName: String,
    val partnerAvatarUrl: String? = null,
)

@Serializable
object Login

@Serializable
data class Register(
    val firstName: String? = null,
    val lastName: String? = null,
    val isFromGoogleLogin: Boolean = false
)