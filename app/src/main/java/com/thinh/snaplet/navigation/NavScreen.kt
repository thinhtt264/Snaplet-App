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
object Login

@Serializable
object Register