package com.bbip.bbipit.domain.usecase

import com.bbip.bbipit.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val authRepository: AuthRepository){

}