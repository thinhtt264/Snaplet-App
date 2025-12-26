package com.thinh.snaplet.data.repository

import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.utils.Logger
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {
    
    override suspend fun getUserProfile(userName: String): Result<UserProfile> {
        return try {
            Logger.d("👤 Fetching user profile for: $userName")
            
            val response = apiService.getUserProfile(userName)
            
            if (response.isSuccessful) {
                val body = response.body()
                
                if (body != null && body.status.code == 200) {
                    val userProfile = body.data
                    Result.success(userProfile)
                } else {
                    val errorMsg = body?.status?.message
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Logger.e("❌ HTTP error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Logger.e("❌ Failed to fetch user profile: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun sendFriendRequest(userId: String): Result<Relationship> {
        return try {
            Logger.d("📤 Sending friend request to user ID: $userId")
            
            val requestBody = mapOf("targetUserId" to userId)
            val response = apiService.sendFriendRequest(requestBody)
            
            if (response.isSuccessful) {
                val body = response.body()
                
                if (body != null && body.status.code == 200) {
                    val relationship = body.data
                    Logger.d("✅ Friend request sent successfully. Relationship ID: ${relationship.id}, Status: ${relationship.status}")
                    Result.success(relationship)
                } else {
                    val errorMsg = body?.status?.message
                    Logger.e("❌ API error: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Logger.e("❌ HTTP error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Logger.e("❌ Failed to send friend request: ${e.message}")
            Result.failure(e)
        }
    }
}

