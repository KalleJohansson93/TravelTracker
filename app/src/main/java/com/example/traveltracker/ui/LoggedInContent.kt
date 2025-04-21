package com.example.traveltracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShowChart // Kräver material-icons-core och extended
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.traveltracker.ui.screens.CountryListScreen // Importera
import com.example.traveltracker.ui.screens.StatisticsScreen // Importera
import com.example.traveltracker.viewmodel.CountryListViewModel // Importera

// Definiera rutter för den inloggade navigationsgrafen
object LoggedInRoutes {
    const val COUNTRY_LIST = "countryList"
    const val STATISTICS = "statistics"
}

// Modell för Bottom Navigation-items
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

// Lista över items i Bottom Navigation
val bottomNavItems = listOf(
    BottomNavItem(LoggedInRoutes.COUNTRY_LIST, Icons.Default.List, "Countries"),
    BottomNavItem(LoggedInRoutes.STATISTICS, Icons.Default.ShowChart, "Statistics")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggedInContent(
    appNavController: NavHostController, // NavController från huvudgrafen (för att navigera bort, t.ex. logout)
    onLogout: () -> Unit // Lambda för att trigga utloggning
) {
    // Egen NavController för den inloggade navigationsgrafen
    val loggedInNavController = rememberNavController()
    // Observera den aktuella destinationen för att veta vilket item som är valt
    val navBackStackEntry by loggedInNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            // Undvik att navigera till samma destination igen
                            if (currentRoute != item.route) {
                                loggedInNavController.navigate(item.route) {
                                    // Pop up till startdestinationen för att undvika en stor backstack
                                    popUpTo(loggedInNavController.graph.startDestinationId) {
                                        saveState = true // Spara state för att kunna återställa skärmen
                                    }
                                    // Undvik flera kopior av samma destination i backstacken
                                    launchSingleTop = true
                                    // Återställ state när man väljer samma item igen
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
        // TopAppBar hanteras bäst i respektive skärm för att kunna visa relevant titel/ikoner
        // Vi skickar in onLogout till skärmarna så de kan lägga till en Logout-knapp i sin TopBar
    ) { innerPadding ->
        // NavHost för navigeringen inom de inloggade sidorna
        NavHost(
            navController = loggedInNavController,
            startDestination = LoggedInRoutes.COUNTRY_LIST, // Börja på landlistan
            modifier = Modifier.padding(innerPadding) // Applicera padding från Scaffold
        ) {
            composable(LoggedInRoutes.COUNTRY_LIST) {
                // Hämta ViewModel här. Standard viewModel() skopar Viewmodel till Composable'n
                // eller dess navgraph entry.
                val countryListViewModel: CountryListViewModel = viewModel()
                CountryListScreen(
                    viewModel = countryListViewModel,
                    onLogoutClick = onLogout // Skicka ner utloggningslambdan
                )
            }
            composable(LoggedInRoutes.STATISTICS) {
                // Här kommer din statistiksida
                // Behöver sannolikt sin egen ViewModel
                StatisticsScreen(
                    onLogoutClick = onLogout // Skicka ner utloggningslambdan
                    // Passa eventuell ViewModel här: viewModel = statisticsViewModel
                )
            }
        }
    }
}