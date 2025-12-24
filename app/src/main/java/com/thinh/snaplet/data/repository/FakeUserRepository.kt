package com.thinh.snaplet.data.repository

import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FakeUserRepository - Mock implementation for development and testing
 * 
 * Enterprise Pattern:
 * - Implementation of UserRepository interface
 * - Simulates API calls with delays
 * - Returns mock data for development
 * 
 * TODO: Replace with real API implementation (UserRepositoryImpl)
 */
@Singleton
class FakeUserRepository @Inject constructor() : UserRepository {
    
    private val mockUsers = mapOf(
        "thuong" to UserProfile(
            userName = "thuong",
            displayName = "Thuong Thuong",
            avatarUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9"
        ),
        "john" to UserProfile(
            userName = "john",
            displayName = "John Doe",
            avatarUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9"
        ),
        "alice" to UserProfile(
            userName = "alice",
            displayName = "Alice Smith",
            avatarUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9"
        ),
        "bob" to UserProfile(
            userName = "bob",
            displayName = "Bob Johnson",
            avatarUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9"
        ),
        "emma" to UserProfile(
            userName = "emma",
            displayName = "Emma Wilson",
            avatarUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9"
        ),
        "david" to UserProfile(
            userName = "david",
            displayName = "David Brown",
            avatarUrl = null  // Test case: no avatar
        )
    )
    
    /**
     * Simulate fetching user profile from API
     * Adds artificial delay to simulate network call
     */
    override suspend fun getUserProfile(userName: String): Result<UserProfile> {
        Logger.d("👤 Fetching user profile for: $userName")
        
        // Simulate network delay (shorter for testing)
        delay(300)
        
        return mockUsers[userName.lowercase()]?.let { user ->
            Logger.d("✅ User profile found: ${user.displayName}")
            Result.success(user)
        } ?: run {
            Logger.w("⚠️ User not found: $userName")
            Result.failure(Exception("User not found: $userName"))
        }
    }
    
    /**
     * Simulate sending friend request
     * Adds artificial delay to simulate network call
     */
    override suspend fun sendFriendRequest(userName: String): Result<Unit> {
        Logger.d("📤 Sending friend request to: $userName")
        
        // Simulate network delay (shorter for testing)
        delay(500)
        
        // Simulate success
        Logger.d("✅ Friend request sent successfully")
        return Result.success(Unit)
    }
}

