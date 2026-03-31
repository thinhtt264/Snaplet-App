package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.model.post.PostReactionUser
import com.thinh.snaplet.domain.model.ReactionUserUi
import javax.inject.Inject

private const val ICON_SEPARATOR = ","

class MapPostReactionUsersUseCase @Inject constructor() {

    operator fun invoke(reactions: List<PostReactionUser>): List<ReactionUserUi> =
        reactions.map { it.toUi() }

    private fun PostReactionUser.toUi(): ReactionUserUi = ReactionUserUi(
        userId = userId,
        displayName = "$firstName $lastName".trim(),
        avatarUrl = avatarUrls.forThumbnail(),
        firstName = firstName,
        reactionIcons = parseIcons(reactionIcon),
    )

    private fun parseIcons(raw: String): List<String> =
        raw.split(ICON_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
