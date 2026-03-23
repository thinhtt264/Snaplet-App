package com.thinh.snaplet.data.repository

import android.webkit.MimeTypeMap
import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.RelationshipCounts
import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.RelationshipWithUserDto
import com.thinh.snaplet.data.model.UpdateRelationshipRequest
import com.thinh.snaplet.data.model.user.AvatarUploadRequest
import com.thinh.snaplet.data.model.user.AvatarUploadRequestResponse
import com.thinh.snaplet.data.model.user.ConfirmAvatarUploadRequest
import com.thinh.snaplet.data.model.user.UpdateDisplayNameRequest
import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.di.BaseOkHttpClient
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.ApiError
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    @BaseOkHttpClient private val baseOkHttpClient: OkHttpClient,
) : UserRepository {

    override suspend fun getUserProfile(userName: String): ApiResult<UserProfile> {
        return safeApiCall(
            apiCall = {
                apiService.getUserProfile(userName)
            }
        )
    }

    override suspend fun sendFriendRequest(userId: String): ApiResult<Relationship> {
        return safeApiCall(
            apiCall = {
                val requestBody = mapOf("targetUserId" to userId)
                apiService.sendFriendRequest(requestBody)
            },
            onSuccess = { relationship ->
                Logger.d("✅ Friend request sent successfully. Relationship ID: ${relationship.id}, Status: ${relationship.status}")
            }
        )
    }

    override suspend fun getRelationshipWithUser(userId: String): ApiResult<Relationship?> {
        return safeApiCall(
            apiCall = {
                apiService.getRelationshipWithUser(mapOf("targetUserId" to userId))
            }
        )
    }

    override suspend fun getRelationshipCounts(): ApiResult<RelationshipCounts> {
        return safeApiCall(
            apiCall = { apiService.getRelationshipCounts() },
        )
    }

    override suspend fun getMyFriendList(): ApiResult<List<RelationshipWithUser>> {
        return safeApiCall(
            apiCall = { apiService.getRelationshipsByStatus(RelationshipStatus.ACCEPTED.value) },
            transform = { list: List<RelationshipWithUserDto> -> list.map { it.toDomain() } }
        )
    }

    override suspend fun getRelationshipsByStatuses(statuses: List<RelationshipStatus>): ApiResult<List<RelationshipWithUser>> {
        if (statuses.isEmpty()) return ApiResult.Success(emptyList())
        val statusesQuery = statuses.joinToString(",") { it.value }
        return safeApiCall(
            apiCall = { apiService.getRelationshipsByStatus(statusesQuery) },
            transform = { list: List<RelationshipWithUserDto> -> list.map { it.toDomain() } }
        )
    }

    override suspend fun acceptFriendRequest(relationshipId: String): ApiResult<Relationship> {
        return safeApiCall(
            apiCall = {
                apiService.updateRelationship(
                    relationshipId,
                    UpdateRelationshipRequest(status = RelationshipStatus.ACCEPTED.value)
                )
            }
        )
    }

    override suspend fun removeFriend(relationshipId: String): ApiResult<Unit> {
        return safeApiCall(
            apiCall = { apiService.removeRelationship(relationshipId) }
        )
    }

    override suspend fun requestAvatarUpload(
        filePath: String,
    ): ApiResult<AvatarUploadRequestResponse> {
        val file = File(filePath)
        if (!file.exists()) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "File does not exist: $filePath"
                )
            )
        }

        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Cannot determine mime type for file: $filePath"
                )
            )

        val fileSize = file.length()
        if (fileSize == 0L) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "File is empty: $filePath"
                )
            )
        }

        val body = AvatarUploadRequest(
            mimeType = mimeType,
            size = fileSize,
        )

        return safeApiCall(
            apiCall = {
                apiService.requestAvatarUpload(body)
            }
        )
    }

    override suspend fun uploadAvatar(
        uploadUrl: String,
        filePath: String,
    ): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext ApiResult.Failure(
                        ApiError(
                            httpCode = 400,
                            message = "File does not exist: $filePath"
                        )
                    )
                }

                val extension = file.extension.lowercase()
                val contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    ?: return@withContext ApiResult.Failure(
                        ApiError(
                            httpCode = 400,
                            message = "Cannot determine content type for file: $filePath"
                        )
                    )

                val requestBody = file.asRequestBody(contentType.toMediaType())

                val request = Request.Builder()
                    .url(uploadUrl)
                    .put(requestBody)
                    .addHeader("Content-Type", contentType)
                    .build()

                val response = baseOkHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Failure(
                        ApiError(
                            httpCode = response.code,
                            message = "Upload failed: ${response.message}"
                        )
                    )
                }
            } catch (e: Exception) {
                ApiResult.Failure(
                    ApiError(
                        httpCode = 500,
                        message = "Upload failed: ${e.message ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    override suspend fun confirmAvatarUpload(
        key: String,
    ): ApiResult<UserProfile> {
        val body = ConfirmAvatarUploadRequest(
            key = key,
        )
        return safeApiCall(
            apiCall = {
                apiService.confirmAvatarUpload(body)
            },
            onSuccess = { updatedProfile ->
                dataStoreManager.saveUserProfile(updatedProfile)
            }
        )
    }

    override suspend fun deleteAvatar(): ApiResult<UserProfile> {
        return safeApiCall(
            apiCall = {
                apiService.deleteAvatar()
            },
            onSuccess = { updatedProfile ->
                dataStoreManager.saveUserProfile(updatedProfile)
            }
        )
    }

    override suspend fun updateDisplayName(
        firstName: String,
        lastName: String,
    ): ApiResult<UserProfile> {
        val body = UpdateDisplayNameRequest(
            firstName = firstName,
            lastName = lastName,
        )
        return safeApiCall(
            apiCall = { apiService.updateDisplayName(body) },
            onSuccess = { updatedProfile ->
                dataStoreManager.saveUserProfile(updatedProfile)
            }
        )
    }

    override fun observeMyUserProfile(): Flow<UserProfile?> {
        return dataStoreManager.getUserProfileFlow()
    }

    override suspend fun getCurrentUserProfile(): UserProfile? {
        return dataStoreManager.loadUserProfile()
    }
}

