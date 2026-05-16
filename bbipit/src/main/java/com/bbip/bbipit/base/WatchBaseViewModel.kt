package com.bbip.bbipit.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 워치 앱 공통 상태 관리를 위한 기본 뷰모델
 */
abstract class WatchBaseViewModel<S>(initialState: S) : ViewModel() {
    // UI 상태 흐름 관리를 위한 가변 상태
    private val _uiState = MutableStateFlow(initialState)
    // 외부에 공개되는 읽기 전용 상태 흐름
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    /**
     * 현재 상태를 새로운 상태로 업데이트
     */
    protected fun updateState(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    /**
     * 현재 상태값 반환
     */
    protected val currentState: S
        get() = uiState.value
}
