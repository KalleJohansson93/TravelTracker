package com.example.traveltracker

import CountryRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel // Kommentera ut om du skickar ViewModel via navigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traveltracker.ui.screens.LoginScreen
import com.example.traveltracker.ui.screens.RegisterScreen
// Importera inte CountryListScreen och StatisticsScreen direkt här längre
// com.example.traveltracker.ui.screens.CountryListScreen
// com.example.traveltracker.ui.screens.StatisticsScreen // Kommer senare
// Import ViewModel om den skickas via navigation: com.example.traveltracker.viewmodel.CountryListViewModel
import com.example.traveltracker.ui.LoggedInContent // Importera den nya Composable'n
import com.example.traveltracker.ui.theme.TravelTrackerTheme
import com.example.traveltracker.viewmodel.CountryListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            TravelTrackerTheme {
                // AppNavigation hanterar nu den övergripande inloggning/utloggning
                AppNavigation()
            }
        }
    }
}

@Composable
fun rememberFirebaseAuthSate(): State<FirebaseUser?> {
    val currentUserState = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    val auth = FirebaseAuth.getInstance()

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUserState.value = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }
    return currentUserState
}

// Definiera rutter som konstanter för att undvika stavfel
object AppRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val LOGGED_IN_CONTENT = "loggedInContent" // En route som wrapper de inloggade sidorna
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentUserState: State<FirebaseUser?> = rememberFirebaseAuthSate()
    val currentUser: FirebaseUser? by currentUserState
    val isLoggedIn = currentUser != null

    LaunchedEffect(isLoggedIn) {
        // Logga ut eventuell utskrift för att se när omdirigering sker
        // android.util.Log.d("AppNavigation", "Login state changed: isLoggedIn = $isLoggedIn")

        if (isLoggedIn) {
            // Om inloggad, navigera till wrappern för inloggat innehåll
            // Ta bort inloggnings- och registreringsskärmarna från backstack
            navController.navigate(AppRoutes.LOGGED_IN_CONTENT) {
                popUpTo(AppRoutes.LOGIN) { inclusive = true }
                popUpTo(AppRoutes.REGISTER) { inclusive = true }
            }
        } else {
            // Om utloggad, navigera till inloggningsskärmen
            // Rensa hela backstacken för att förhindra att man backar till inloggat innehåll
            navController.navigate(AppRoutes.LOGIN) {
                // Rensa backstacken upp till startdestinationen (login i detta fall)
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    // Notera att Scaffolden nu *inte* wrappar NavHosten, eftersom LoggedInContent
    // kommer att ha sin egen Scaffold med BottomAppBar.
    NavHost(
        navController = navController,
        startDestination = AppRoutes.LOGIN // Alltid starta på login, LaunchedEffect hanterar omdirigering
        // Modifier appliceras inuti respektive Composable (t.ex. LoginScreen, LoggedInContent)
        // modifier = Modifier.padding(innerPadding) // Ta bort detta
    ) {
        composable(AppRoutes.LOGIN) {
            // Inga Scaffold här, LoginScreen har sin egen layout
            LoginScreen(
               onNavigateToRegister = { navController.navigate(AppRoutes.REGISTER) }
                // onLoginSuccess är inte nödvändigt här längre, LaunchedEffect hanterar det
            )
        }
        composable(AppRoutes.REGISTER) {
            // Placeholder för registersidan, behöver anrop till navController här också
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(AppRoutes.LOGIN) }
                // onRegistrationSuccess är inte nödvändigt här, LaunchedEffect hanterar det
            )
        }
        // Denna Composable agerar som en wrapper för allt inloggat innehåll
        composable(AppRoutes.LOGGED_IN_CONTENT) {
            // Här skapar vi vår LoggedInContent Composable
            // Den får navigationskontrollen från huvudnavigeringen
            // och en lambda för utloggning
            LoggedInContent(
                appNavController = navController, // Passa huvudkontrollen till LoggedInContent
                onLogout = {
                    // Utför utloggningen via Firebase Auth
                    FirebaseAuth.getInstance().signOut()
                    // LaunchedEffect i AppNavigation kommer att upptäcka state-ändringen
                    // och navigera till login
                }
            )
        }
    }
}

// Lägg till eller uppdatera din RegisterScreen Composable för att hantera navigation tillbaka
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit
    // Lägg till andra parametrar som behövs för registrering (viewModel, etc.)
) {
    // Din UI för registreringsskärmen
    // Inkludera en knapp eller länk som anropar onNavigateToLogin()
    // Scaffold kan finnas här
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        // Din registrerings-UI
        // Exempel: Knapp för att gå tillbaka till inloggning
        Button(onClick = onNavigateToLogin, modifier = Modifier.padding(innerPadding)) {
            Text("Already have an account? Login")
        }
        // Lägg till din registreringsform etc. här
    }

}