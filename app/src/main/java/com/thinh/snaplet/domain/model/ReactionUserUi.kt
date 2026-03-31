package com.thinh.snaplet.domain.model

data class ReactionUserUi(
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val firstName: String,
    val reactionIcons: List<String>,
)
