package com.thinh.snaplet.domain.feed

import com.thinh.snaplet.data.model.post.NewPostUpdate
import com.thinh.snaplet.data.repository.post.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNewPostEventUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(): Flow<NewPostUpdate> = postRepository.newPostMessages
}
