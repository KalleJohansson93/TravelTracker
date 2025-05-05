package com.example.traveltracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.traveltracker.data.UserProfile // Importera UserProfile data klassen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser // Importera FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore // Importera Firestore
import com.google.firebase.firestore.SetOptions // Importera SetOptions för merge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // Import för att använda await på Tasks


// *** UPPDATERA UI STATE ***
data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false
    // Användarnamnsfältet hanteras bäst i UI-state (composable remember)
    // val username: String = "" // Valfritt om du vill ha det här istället för i composable state
)

// *** SKAPA FACTORY FÖR VIEWMODEL MED BEROENDEN ***
class LoginViewModelFactory(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore // Lägg till Firestore här
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(firebaseAuth, firestore) as T // Skicka med beroenden till ViewModel
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


// *** UPPDATERA VIEWMODEL ***
class LoginViewModel(
    private val auth: FirebaseAuth, // Ta emot FirebaseAuth
    private val firestore: FirebaseFirestore // *** TA EMOT FIRESTORE ***
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    // Inget behov av att instansiera auth och firestore här om de skickas in


    fun login(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password)
                    .await() // Använd await för att vänta på resultatet asynkront
                // Om await lyckas, är användaren inloggad
                _uiState.update { it.copy(isLoading = false, isUserLoggedIn = true) }

            } catch (e: Exception) {
                // Hantera fel vid inloggning
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // *** UPPDATERA REGISTER FUNKTIONEN ***
    // Lägg till username som parameter
    fun register(email: String, password: String, username: String, onRegisterSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // 1. Skapa användaren i Firebase Authentication
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser: FirebaseUser? = authResult.user // Få den nyskapade användaren

                if (firebaseUser != null) {
                    val userId = firebaseUser.uid
                    // 2. Spara användarnamnet i Firestore
                    saveUsernameToFirestore(userId, username)
                        .await() // Vänta på att sparandet i Firestore blir klart

                    // 3. Om både Auth och Firestore lyckas
                    _uiState.update { it.copy(isLoading = false, isUserLoggedIn = true) }
                    onRegisterSuccess() // Utför navigering eller annan callback

                } else {
                    // Detta borde inte hända om createUserWithEmailAndPassword lyckas men användaren är null
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Registration failed: User is null") }
                }

            } catch (e: Exception) {
                // Hantera fel (Auth-fel eller Firestore-fel)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // *** NY FUNKTION FÖR ATT SPARA ANVÄNDARNAMN I FIRESTORE ***
    private fun saveUsernameToFirestore(userId: String, username: String) =
        firestore.collection("users") // Gå till users samlingen
            .document(userId) // Gå till användarens specifika dokument (UID som ID)
            // Sätt fältet 'username'. Använd merge=true för att inte skriva över andra fält (som visitedCountriesCount)
            .set(UserProfile(username = username), SetOptions.merge())


    fun resetState() {
        _uiState.update { LoginUiState() }
    }
}