package com.thinh.snaplet.domain.post

import android.net.Uri
import com.thinh.snaplet.platform.share.ShareContent
import com.thinh.snaplet.utils.InviteConstants
import javax.inject.Inject

class BuildPostShareContentUseCase @Inject constructor() {
    operator fun invoke(firstName: String, postId: String): ShareContent {
        val shareUrl = Uri.parse(InviteConstants.BASE_URL).buildUpon()
            .appendQueryParameter("postId", postId)
            .build()
            .toString()

        val text = buildString {
            append("Khoảnh khắc của ")
            append(firstName)
            appendLine()
            append(shareUrl)
        }

        return ShareContent(str = text)
    }
}
