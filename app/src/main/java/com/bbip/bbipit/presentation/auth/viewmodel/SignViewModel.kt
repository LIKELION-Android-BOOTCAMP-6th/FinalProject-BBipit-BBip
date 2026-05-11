package com.bbip.bbipit.presentation.auth.viewmodel

import androidx.lifecycle.ViewModel
import com.bbip.bbipit.domain.repository.UserRepository
import com.bbip.bbipit.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val userRepository: UserRepository,
) : ViewModel() {
}