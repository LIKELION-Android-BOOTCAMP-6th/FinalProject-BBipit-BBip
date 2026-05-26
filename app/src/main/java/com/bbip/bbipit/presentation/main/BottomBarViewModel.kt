package com.bbip.bbipit.presentation.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject


@HiltViewModel
class BottomBarViewModel @Inject constructor(): ViewModel() {

    private val _isDrawerShown = MutableStateFlow(false)
    val isDrawerShown: StateFlow<Boolean> = _isDrawerShown.asStateFlow()

    fun onUpdateDrawerShown(value: Boolean) = _isDrawerShown.update { value }
}