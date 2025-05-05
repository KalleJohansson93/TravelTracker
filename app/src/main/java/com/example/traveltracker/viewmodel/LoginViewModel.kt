package com.example.traveltracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.traveltracker.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


// *** UPPDATERA UI STATE ***
data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false
)

// *** SKAPA FACTORY FÖR VIEWMODEL MED BEROENDEN ***
class LoginViewModelFactory(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(firebaseAuth, firestore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


// *** UPPDATERA VIEWMODEL ***
class LoginViewModel(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password)
                    .await()
                _uiState.update { it.copy(isLoading = false, isUserLoggedIn = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun register(email: String, password: String, username: String, onRegisterSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser: FirebaseUser? = authResult.user

                if (firebaseUser != null) {
                    val userId = firebaseUser.uid
                    saveUsernameToFirestore(userId, username)
                        .await()

                    _uiState.update { it.copy(isLoading = false, isUserLoggedIn = true) }
                    onRegisterSuccess()

                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Registration failed: User is null") }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    private fun saveUsernameToFirestore(userId: String, username: String) =
        firestore.collection("users")
            .document(userId)
            .set(UserProfile(username = username), SetOptions.merge())


    fun resetState() {
        _uiState.update { LoginUiState() }
    }
}