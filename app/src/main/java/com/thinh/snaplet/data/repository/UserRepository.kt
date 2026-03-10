package com.thinh.snaplet.data.repository

import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.data.model.user.AvatarUploadRequestResponse
import com.thinh.snaplet.utils.network.ApiResult
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun getUserProfile(userName: String): ApiResult<UserProfile>
    
    suspend fun sendFriendRequest(userId: String): ApiResult<Relationship>

    suspend fun getRelationshipWithUser(userId: String): ApiResult<Relationship?>

    suspend fun getFriendsCount(): ApiResult<Int>

    suspend fun getMyFriendList(): ApiResult<List<RelationshipWithUser>>

    suspend fun getRelationshipsByStatuses(statuses: List<RelationshipStatus>): ApiResult<List<RelationshipWithUser>>

    suspend fun acceptFriendRequest(relationshipId: String): ApiResult<Relationship>

    suspend fun removeFriend(relationshipId: String): ApiResult<Unit>

    suspend fun requestAvatarUpload(
        filePath: String,
    ): ApiResult<AvatarUploadRequestResponse>

    suspend fun uploadAvatar(
        uploadUrl: String,
        filePath: String,
    ): ApiResult<Unit>

    suspend fun confirmAvatarUpload(
        key: String,
    ): ApiResult<UserProfile>

    suspend fun deleteAvatar(): ApiResult<UserProfile>

    fun observeMyUserProfile(): Flow<UserProfile?>
    
    suspend fun getCurrentUserProfile(): UserProfile?

    suspend fun updateDisplayName(
        firstName: String,
        lastName: String,
    ): ApiResult<UserProfile>
}

