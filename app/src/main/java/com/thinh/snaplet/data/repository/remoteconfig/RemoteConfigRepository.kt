package com.thinh.snaplet.data.repository.remoteconfig

interface RemoteConfigRepository {
    suspend fun fetchMaintenanceConfig(): MaintenanceConfig
}

data class MaintenanceConfig(
    val isEnabled: Boolean,
    val estimatedEndTime: String,
)
