package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.domain.model.PostAction
import javax.inject.Inject

/**
 * Determines which actions are available for a post based on ownership and upload state.
 * UI layer maps [PostAction] to [SheetOption] (labels, colors, click handlers).
 */
class GetAvailablePostActionsUseCase @Inject constructor() {

    /**
     * @param post the post to show options for
     * @param isUploading true if this post is currently uploading (hides Delete for own posts)
     */
    operator fun invoke(post: Post, isUploading: Boolean): List<PostAction> = buildList {
        add(PostAction.Share)
        add(PostAction.Download)
        if (post.isOwnPost && !isUploading) {
            add(PostAction.Delete)
        }
        if (!post.isOwnPost) {
            add(PostAction.Report)
        }
        add(PostAction.Cancel)
    }
}
