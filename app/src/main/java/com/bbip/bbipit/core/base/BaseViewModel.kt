package com.bbip.bbipit.core.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI 상태 관리를 위한 기본 ViewModel 클래스
 *
 * @param S UI 상태 데이터 클래스 타입
 */
abstract class BaseViewModel<S>(initialState: S) : ViewModel() {

    // UI 상태 스트림
    private val _uiState = MutableStateFlow(initialState)

    // UI 관찰을 위한 읽기 전용 상태
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    /**
     * UI 상태 업데이트 실행
     *
     * @param reducer 상태 변경 로직
     */
    protected fun updateState(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    // 현재 상태 조회
    protected val currentState: S
        get() = uiState.value
}