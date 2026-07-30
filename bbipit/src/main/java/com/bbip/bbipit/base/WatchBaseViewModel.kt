package com.bbip.bbipit.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 워치 앱 공통 상태 관리를 위한 기본 ViewModel 클래스
 */
abstract class WatchBaseViewModel<S>(initialState: S) : ViewModel() {

    // UI 상태 흐름 관리를 위한 가변 상태 변수
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    /**
     * 현재 상태를 새로운 상태로 업데이트하는 함수
     */
    protected fun updateState(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    /**
     * 현재 상태값을 반환하는 프로퍼티
     */
    protected val currentState: S
        get() = uiState.value
}