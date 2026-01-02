package com.thinh.snaplet.data.datasource.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snaplet_preferences")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context, private val gson: Gson
) {

    private val dataStore = context.dataStore

    private val accessTokenKey = stringPreferencesKey(DataStoreKeys.SessionKeys.ACCESS_TOKEN)
    private val refreshTokenKey = stringPreferencesKey(DataStoreKeys.SessionKeys.REFRESH_TOKEN)

    private val userProfileKey = stringPreferencesKey(DataStoreKeys.UserProfileKeys.PROFILE)

    private val currentAccessToken = AtomicReference<String?>(null)
    private val currentRefreshToken = AtomicReference<String?>(null)

    init {
        CoroutineScope(Dispatchers.Default).launch { reHydrate() }
    }

    private suspend fun reHydrate() {
        try {
            val preferences = dataStore.data.first()
            currentAccessToken.set(preferences[accessTokenKey])
            currentRefreshToken.set(preferences[refreshTokenKey])
        } catch (e: Exception) {
            Logger.e("Failed to init cache: ${e.message}")
        }
    }

    suspend fun saveAccessToken(token: String) {
        currentAccessToken.set(token)
        dataStore.edit { preferences ->
            preferences[accessTokenKey] = token
        }
        Logger.d("💾 Access token saved")
    }

    fun getAccessToken(): String? {
        return currentAccessToken.get()
    }

    fun getRefreshToken(): String? {
        return currentRefreshToken.get()
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        currentAccessToken.set(accessToken)
        currentRefreshToken.set(refreshToken)

        dataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
        }
    }

    suspend fun clearSession() {
        currentAccessToken.set(null)
        currentRefreshToken.set(null)

        dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(refreshTokenKey)
        }
    }

    suspend fun saveUserProfile(user: UserProfile) {
        try {
            val userJson = gson.toJson(user)
            dataStore.edit { preferences ->
                preferences[userProfileKey] = userJson
            }
        } catch (e: Exception) {
            Logger.e("❌ Failed to save user profile: ${e.message}")
        }
    }

    suspend fun getUserProfile(): UserProfile? {
        return try {
            val preferences = dataStore.data.first()
            val userJson = preferences[userProfileKey] ?: return null

            gson.fromJson(userJson, UserProfile::class.java)
        } catch (e: Exception) {
            Logger.e("❌ Failed to get user profile: ${e.message}")
            null
        }
    }

    fun getUserProfileFlow(): Flow<UserProfile?> {
        return dataStore.data.map { preferences ->
            try {
                val userJson = preferences[userProfileKey] ?: return@map null
                gson.fromJson(userJson, UserProfile::class.java)
            } catch (e: Exception) {
                Logger.e("❌ Failed to parse user profile from JSON: ${e.message}")
                null
            }
        }
    }

    suspend fun clearUserProfile() {
        dataStore.edit { preferences ->
            preferences.remove(userProfileKey)
        }
        Logger.d("🗑️ User profile cleared")
    }

    suspend fun clearAll() {
        clearSession()
        clearUserProfile()
        Logger.d("🗑️ All data cleared")
    }
}

