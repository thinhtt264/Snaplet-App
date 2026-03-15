package com.thinh.snaplet.data.repository.post

import com.thinh.snaplet.data.model.post.NewPostUpdate
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    val newPostMessages: Flow<NewPostUpdate>
}
