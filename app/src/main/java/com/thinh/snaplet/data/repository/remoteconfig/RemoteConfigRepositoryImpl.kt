package com.thinh.snaplet.data.repository.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.thinh.snaplet.BuildConfig
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val KEY_MAINTENANCE_ENABLED = "is_maintenance_enabled"
private const val KEY_MAINTENANCE_END_TIME = "maintenance_end_time"

class RemoteConfigRepositoryImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) : RemoteConfigRepository {

    override suspend fun fetchMaintenanceConfig(): MaintenanceConfig {
        if (BuildConfig.DEBUG) return MaintenanceConfig(isEnabled = false, estimatedEndTime = "")
        return try {
            remoteConfig.fetchAndActivate().await()
            MaintenanceConfig(
                isEnabled = remoteConfig.getBoolean(KEY_MAINTENANCE_ENABLED),
                estimatedEndTime = remoteConfig.getString(KEY_MAINTENANCE_END_TIME),
            )
        } catch (e: Exception) {
            Logger.e("RemoteConfig fetch failed: ${e.message}")
            MaintenanceConfig(isEnabled = false, estimatedEndTime = "")
        }
    }
}
