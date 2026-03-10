package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.media.ImageSizes
import com.thinh.snaplet.data.model.media.ImageTransform
import com.thinh.snaplet.data.model.media.Media
import com.thinh.snaplet.data.model.user.UserProfile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Builds a temporary [Post] for optimistic UI before upload completes.
 * Pure data transformation – no I/O.
 */
class CreateTempPostUseCase @Inject constructor() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)

    operator fun invoke(
        id: String,
        imagePath: String,
        userProfile: UserProfile,
        transform: ImageTransform,
        caption: String? = null
    ): Post {
        val file = File(imagePath)
        val fileUri = "file://${file.absolutePath}"

        val tempMedia = Media(
            id = "temp_media_$id",
            type = "image",
            images = ImageSizes(original = fileUri),
            transform = transform,
            ownerId = userProfile.id
        )

        return Post(
            id = id,
            userId = userProfile.id,
            username = userProfile.userName,
            firstName = userProfile.firstName,
            lastName = userProfile.lastName,
            avatarUrls = userProfile.avatarUrls,
            media = listOf(tempMedia),
            caption = caption,
            visibility = "friend-only",
            createdAt = dateFormat.format(System.currentTimeMillis()),
            isOwnPost = true
        )
    }
}