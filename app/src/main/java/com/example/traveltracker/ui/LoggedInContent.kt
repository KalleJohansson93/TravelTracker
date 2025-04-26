package com.example.traveltracker.ui

import CountryRepository
import LocalCountryDataSource
import android.content.Context // Behövs för att få Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember // Använd remember för att inte skapa instanser i varje recomposition
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext // För att få Context i Composable
import androidx.lifecycle.viewmodel.compose.viewModel // Används med factory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// Importera dina nya Data Sources och Repository
import com.example.traveltracker.data.FirestoreUserCountryDataSource
import com.example.traveltracker.ui.screens.CountryListScreen
import com.example.traveltracker.ui.screens.StatisticsScreen

// Importera din Factory
import com.example.traveltracker.viewmodel.CountryListViewModelFactory
import com.example.traveltracker.viewmodel.CountryListViewModel // Din ViewModel

// Importera Firebase instanser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.FirebaseFirestoreSettings // Importera för offline

object LoggedInRoutes {
    const val COUNTRY_LIST = "countryList"
    const val STATISTICS = "statistics"
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(LoggedInRoutes.COUNTRY_LIST, Icons.Default.List, "Countries"),
    BottomNavItem(LoggedInRoutes.STATISTICS, Icons.Default.ShowChart, "Statistics")
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggedInContent(
    appNavController: NavHostController, // Används om du vill navigera UTANFÖR logged-in grafen
    onLogout: () -> Unit // Lambda för utloggning
) {
    val loggedInNavController = rememberNavController()
    val navBackStackEntry by loggedInNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // --- SKAPA DEPENDENCIES OCH FACTORY HÄR ---
    // Använd remember för att dessa instanser ska överleva recompositions
    val context = LocalContext.current
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val firebaseApp = remember { FirebaseApp.getInstance() }
    val firestore = remember { Firebase.firestore(firebaseApp, "traveltracker") }

    // *** VIKTIGT: Aktivera Offline Persistence här eller i Application-klassen ***
    // Gör detta bara EN gång i appens livstid
    remember {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Aktivera offline persistence
                .build()
            firestore.firestoreSettings = settings
            // Logga eller hantera om inställningarna redan är satta
            android.util.Log.d("FirestoreSetup", "Offline persistence enabled")
        } catch (e: Exception) {
            // Hantera fallet om persistence redan är aktiverat (sker om du sätter det flera gånger)
            android.util.Log.e("FirestoreSetup", "Error enabling offline persistence: ${e.message}")
        }
        // Returnera något värde för remember att cacha, t.ex. true
        true
    }


    // Skapa datakällor
    val localCountryDataSource = remember { LocalCountryDataSource(context) }
    val firestoreUserCountryDataSource = remember { FirestoreUserCountryDataSource(firestore, firebaseAuth) }

    // Skapa repository
    val countryRepository = remember { CountryRepository(localCountryDataSource, firestoreUserCountryDataSource) }

    // Skapa ViewModel Factory
    val countryListViewModelFactory = remember { CountryListViewModelFactory(countryRepository, firebaseAuth) }

    // --- ANVÄND FACTORYN NÄR VIEWMODEL SKAPAS ---
    Scaffold(
        bottomBar = {
            // ... (Bottom Navigation Bar code, oförändrad)
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                loggedInNavController.navigate(item.route) {
                                    popUpTo(loggedInNavController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = loggedInNavController,
            startDestination = LoggedInRoutes.COUNTRY_LIST,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(LoggedInRoutes.COUNTRY_LIST) {
                // Använd viewModel() med din manuella factory
                val countryListViewModel: CountryListViewModel = viewModel(factory = countryListViewModelFactory)

                CountryListScreen(
                    viewModel = countryListViewModel,
                    onLogoutClick = onLogout
                )
            }
            composable(LoggedInRoutes.STATISTICS) {
                // Här behöver du en StatisticsViewModelFactory om den har beroenden
                // val statisticsViewModel: StatisticsViewModel = viewModel(...)
                StatisticsScreen(
                    onLogoutClick = onLogout
                )
            }
        }
    }
}