package com.thinh.snaplet.data.datasource.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.GsonHolder.gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snaplet_preferences")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore

    private val accessTokenKey = stringPreferencesKey(DataStoreKeys.SessionKeys.ACCESS_TOKEN)
    private val refreshTokenKey = stringPreferencesKey(DataStoreKeys.SessionKeys.REFRESH_TOKEN)

    private val userProfileKey = stringPreferencesKey(DataStoreKeys.UserProfileKeys.PROFILE)
    private val fingerprintKey = stringPreferencesKey(DataStoreKeys.DeviceKeys.FINGERPRINT)

    private val currentAccessToken = AtomicReference<String?>(null)
    private val currentRefreshToken = AtomicReference<String?>(null)

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

    suspend fun loadUserProfile(): UserProfile? {
        return try {
            val preferences = dataStore.data.first()
            val userJson = preferences[userProfileKey] ?: return null

            gson.fromJson(userJson, UserProfile::class.java)
        } catch (_: Exception) {
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
        clearFingerprint()
        Logger.d("🗑️ All data cleared")
    }

    suspend fun loadAccessToken(): String? {
        return try {
            val preferences = dataStore.data.first()
            val token = preferences[accessTokenKey]
            if (token != null) {
                currentAccessToken.set(token)
            }
            token
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadRefreshToken(): String? {
        return try {
            val preferences = dataStore.data.first()
            val token = preferences[refreshTokenKey]
            if (token != null) {
                currentRefreshToken.set(token)
            }
            token
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveFingerprint(fingerprint: String) {
        dataStore.edit { preferences ->
            preferences[fingerprintKey] = fingerprint
        }
    }

    suspend fun loadFingerprint(): String? {
        return try {
            val preferences = dataStore.data.first()
            preferences[fingerprintKey]
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearFingerprint() {
        dataStore.edit { preferences ->
            preferences.remove(fingerprintKey)
        }
    }
}

