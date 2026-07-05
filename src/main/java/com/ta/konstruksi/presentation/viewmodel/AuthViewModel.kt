package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.UserSession
import com.ta.konstruksi.domain.repository.AuthRepository
import com.ta.konstruksi.domain.usecase.auth.LoginUseCase
import com.ta.konstruksi.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val session: UserSession) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    // Pengecekan sesi saat aplikasi dibuka: null = sedang cek, true = sudah login, false = belum.
    // Dipakai untuk melewati LoginScreen bila sesi tersimpan masih ada (mendukung mode luring).
    private val _startupLoggedIn = MutableStateFlow<Boolean?>(null)
    val startupLoggedIn: StateFlow<Boolean?> = _startupLoggedIn

    init {
        viewModelScope.launch {
            _startupLoggedIn.value = authRepository.getLocalSession() != null
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                loginUseCase(email, password).fold(
                    onSuccess = { _uiState.value = AuthUiState.Success(it) },
                    onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Login gagal") }
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Login gagal")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
