package com.example.traveltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traveltracker.ui.screens.LoginScreen
import com.example.traveltracker.ui.screens.RegisterScreen
import com.example.traveltracker.ui.LoggedInContent
import com.example.traveltracker.ui.theme.TravelTrackerTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            TravelTrackerTheme {
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

object AppRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val LOGGED_IN_CONTENT = "loggedInContent"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentUserState: State<FirebaseUser?> = rememberFirebaseAuthSate()
    val currentUser: FirebaseUser? by currentUserState
    val isLoggedIn = currentUser != null

    LaunchedEffect(isLoggedIn) {

        if (isLoggedIn) {
            navController.navigate(AppRoutes.LOGGED_IN_CONTENT) {
                popUpTo(AppRoutes.LOGIN) { inclusive = true }
                popUpTo(AppRoutes.REGISTER) { inclusive = true }
            }
        } else {
            navController.navigate(AppRoutes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.LOGIN
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
               onNavigateToRegister = { navController.navigate(AppRoutes.REGISTER) }
            )
        }
        composable(AppRoutes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(AppRoutes.LOGIN) },
                onRegisterSuccess = { navController.navigate(AppRoutes.LOGIN) }
            )
        }
        composable(AppRoutes.LOGGED_IN_CONTENT) {
            LoggedInContent(
                appNavController = navController,
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                }
            )
        }
    }
}