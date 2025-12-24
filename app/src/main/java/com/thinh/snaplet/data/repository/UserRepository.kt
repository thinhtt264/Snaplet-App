package com.thinh.snaplet.data.repository

import com.thinh.snaplet.data.model.UserProfile

/**
 * UserRepository - Interface for user data operations
 * 
 * Enterprise Pattern:
 * - Repository pattern for data abstraction
 * - Can be implemented with different data sources (API, local DB, cache)
 * - Easily mockable for testing
 * 
 * Responsibilities:
 * - Fetch user profile data
 * - Handle data caching and synchronization
 * - Abstract data source implementation details
 */
interface UserRepository {
    /**
     * Get user profile by username
     * 
     * @param userName Username to fetch profile for
     * @return Result containing UserProfile or error
     */
    suspend fun getUserProfile(userName: String): Result<UserProfile>
    
    /**
     * Send friend request to user
     * 
     * @param userName Username to send request to
     * @return Result indicating success or failure
     */
    suspend fun sendFriendRequest(userName: String): Result<Unit>
}

