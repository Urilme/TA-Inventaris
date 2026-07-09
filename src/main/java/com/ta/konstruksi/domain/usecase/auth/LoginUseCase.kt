package com.ta.konstruksi.domain.usecase.auth

import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<UserSession> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Username dan kata sandi tidak boleh kosong"))
        }
        return authRepository.login(username, password)
    }
}
