package com.bbip.bbipit.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbip.bbipit.core.base.LifeCycleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NetworkWarningBannerViewModel @Inject constructor(
    private val lifeCycleManager: LifeCycleManager
) : ViewModel() {
    val isNetworkConnected: StateFlow<Boolean> = lifeCycleManager.isNetworkConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0),
            initialValue = true
        )
}