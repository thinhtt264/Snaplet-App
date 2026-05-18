package com.thinh.snaplet.ui.app

data class AppUiState(
    val startDestination: Any? = null,
    val isLoading: Boolean = true,
    val isMaintenance: Boolean = false,
    val maintenanceEndTime: String = "",
)