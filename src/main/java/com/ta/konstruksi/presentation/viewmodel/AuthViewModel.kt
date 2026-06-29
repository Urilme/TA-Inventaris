package com.ta.konstruksi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ta.konstruksi.domain.model.UserSession
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
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

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
