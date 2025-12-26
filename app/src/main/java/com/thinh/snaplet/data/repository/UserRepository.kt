package com.thinh.snaplet.data.repository

import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.UserProfile

interface UserRepository {

    suspend fun getUserProfile(userName: String): Result<UserProfile>
    
    suspend fun sendFriendRequest(userId: String): Result<Relationship>
}

