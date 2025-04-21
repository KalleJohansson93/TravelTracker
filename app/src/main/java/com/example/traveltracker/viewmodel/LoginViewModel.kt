package com.example.traveltracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun login(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _uiState.update { it.copy(isLoading = false, isUserLoggedIn = true) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, errorMessage = task.exception?.message) }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun register(email: String, password: String, onRegisterSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _uiState.update { it.copy(isLoading = false, isUserLoggedIn = true) }
                            onRegisterSuccess() // Callback för att navigera vidare
                        } else {
                            _uiState.update { it.copy(isLoading = false, errorMessage = task.exception?.message) }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun resetState() {
        _uiState.update { LoginUiState() }
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false
)