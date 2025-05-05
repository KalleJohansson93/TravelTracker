package com.example.traveltracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.traveltracker.data.CountryRepository
import com.example.traveltracker.data.FirestoreGlobalStatsDataSource

// Importera dina nya Data Sources och Repository
import com.example.traveltracker.data.FirestoreUserCountryDataSource
import com.example.traveltracker.data.FirestoreUserDataDataSource
import com.example.traveltracker.data.LocalCountryDataSource
import com.example.traveltracker.data.StatisticsRepository
import com.example.traveltracker.ui.screens.CountryListScreen
import com.example.traveltracker.ui.screens.StatisticsScreen

// Importera din Factory
import com.example.traveltracker.viewmodel.CountryListViewModelFactory
import com.example.traveltracker.viewmodel.CountryListViewModel
import com.example.traveltracker.viewmodel.StatisticsViewModel
import com.example.traveltracker.viewmodel.StatisticsViewModelFactory

// Importera Firebase instanser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

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

@Composable
fun LoggedInContent(
    onLogout: () -> Unit,
    firestore: FirebaseFirestore
) {
    val loggedInNavController = rememberNavController()
    val navBackStackEntry by loggedInNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // --- SKAPA DEPENDENCIES OCH FACTORY HÄR ---
    val context = LocalContext.current
    val firebaseAuth = remember { FirebaseAuth.getInstance() }

    remember {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
            android.util.Log.d("FirestoreSetup", "Offline persistence enabled")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSetup", "Error enabling offline persistence: ${e.message}")
        }
        // Returnera något värde för remember att cacha, t.ex. true
        true
    }


    // Skapa datakällor
    val localCountryDataSource = remember { LocalCountryDataSource(context) }
    val firestoreUserCountryDataSource = remember { FirestoreUserCountryDataSource(firestore, firebaseAuth) }
    val firestoreUserDataDataSource = remember { FirestoreUserDataDataSource(firestore, firebaseAuth) }
    val firestoreGlobalStatsDataSource = remember { FirestoreGlobalStatsDataSource(firestore) }


    // Skapa repository
    val countryRepository = remember { CountryRepository(localCountryDataSource, firestoreUserCountryDataSource) }
    val statisticsRepository = remember { // Ny
        StatisticsRepository(
            localDataSource = localCountryDataSource,
            firestoreUserCountryDataSource = firestoreUserCountryDataSource,
            firestoreUserDataDataSource = firestoreUserDataDataSource,
            firestoreGlobalStatsDataSource = firestoreGlobalStatsDataSource
        )
    }

    // Skapa ViewModel Factory
    val countryListViewModelFactory = remember { CountryListViewModelFactory(countryRepository, firebaseAuth) }
    val statisticsViewModelFactory = remember { StatisticsViewModelFactory(statisticsRepository) } // Ny


    // --- ANVÄND FACTORYN NÄR VIEWMODEL SKAPAS ---
    Scaffold(
        bottomBar = {
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
                val countryListViewModel: CountryListViewModel = viewModel(factory = countryListViewModelFactory)
                CountryListScreen(
                    viewModel = countryListViewModel,
                    onLogoutClick = onLogout
                )
            }
            composable(LoggedInRoutes.STATISTICS) {
                val statisticsViewModel: StatisticsViewModel = viewModel(factory = statisticsViewModelFactory)
                StatisticsScreen(
                    viewModel = statisticsViewModel,
                    onLogoutClick = onLogout
                )
            }
        }
    }
}