package com.thinh.snaplet.ui.screens.home

import com.thinh.snaplet.utils.permission.Permission

/**
 * UI Events emitted from ViewModel to View
 * Following MVVM + Clean Architecture principles
 * 
 * ViewModel decides WHAT should happen
 * View executes HOW it happens
 */
sealed class HomeUiEvent {
    /**
     * Request permission from user
     * View will show Android system permission dialog
     */
    data class RequestPermission(val permission: Permission) : HomeUiEvent()
    
    /**
     * Show error message to user
     */
    data class ShowError(val message: String) : HomeUiEvent()
    
    /**
     * Show success message to user
     */
    data class ShowSuccess(val message: String) : HomeUiEvent()
}

