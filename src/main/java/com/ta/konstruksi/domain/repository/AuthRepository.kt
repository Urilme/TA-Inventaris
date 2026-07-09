package com.ta.konstruksi.domain.repository

import com.ta.konstruksi.domain.model.UserSession

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<UserSession>
    suspend fun logout()
    suspend fun getLocalSession(): UserSession?
    suspend fun isSessionValid(): Boolean
}
