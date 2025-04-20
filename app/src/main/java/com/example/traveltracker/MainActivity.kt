package com.example.traveltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traveltracker.ui.screens.LoginScreen
import com.example.traveltracker.ui.screens.RegisterScreen
import com.example.traveltracker.ui.theme.TravelTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TravelTrackerTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login", // Ange startdestinationen till inloggning
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("countryList") {
                                        popUpTo("login") { inclusive = true } // Förhindra att man kan gå tillbaka till login
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate("countryList") {
                                        popUpTo("register") { inclusive = true } // Förhindra att man kan gå tillbaka till registrering
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack() // Gå tillbaka till föregående skärm (login)
                                }
                            )
                        }
                        composable("countryList") {
                            CountryListScreen() // Skapa en placeholder för din inloggade vy
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountryListScreen() {
    Text(text = "Inloggad vy med länder") // Placeholder för din inloggade vy
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TravelTrackerTheme {
        // Du kan förhandsgranska dina LoginScreen eller RegisterScreen här istället för Greeting
        // LoginScreen(onLoginSuccess = {}, onNavigateToRegister = {})
        // RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {})
        Text("Förhandsvisning") // Enkel förhandsvisning i brist på specifik skärm
    }
}