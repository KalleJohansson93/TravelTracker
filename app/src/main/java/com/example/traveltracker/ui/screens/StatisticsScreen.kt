package com.example.traveltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn // Om topplistorna blir långa
import androidx.compose.foundation.lazy.items // Om topplistorna blir långa
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // Observera Flow
import androidx.compose.runtime.getValue // Observera Flow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight // För fet text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // För textstorlek
import com.example.traveltracker.viewmodel.StatisticsViewModel // Din Statistik ViewModel
import com.example.traveltracker.data.CountryDisplayStat // Hjälpklass för global statistik i UI
import com.example.traveltracker.data.StatisticsData // Din Statistik Data Class


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    // *** TA EMOT VIEWMODEL SOM PARAMETER ***
    viewModel: StatisticsViewModel,
    onLogoutClick: () -> Unit // Lambda för utloggning
) {
    // Observera statistik-statet från ViewModel
    val uiState by viewModel.statisticsUiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Travel Statistics") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        // Din UI för statistiksidan
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (uiState.isLoading || uiState.errorMessage != null) Arrangement.Center else Arrangement.Top // Centrera om laddar/fel
        ) {
            when {
                // Visa laddningsindikator
                uiState.isLoading -> {
                    CircularProgressIndicator()
                    Text("Loading statistics...")
                }
                // Visa felmeddelande
                uiState.errorMessage != null -> {
                    Text(
                        text = "Error: ${uiState.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Visa statistiken när den är laddad
                else -> {
                    // Personlig Statistik
                    Text(
                        text = "Statistics for ${uiState.username}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Countries Visited: ${uiState.visitedCountriesCount} / ${uiState.totalCountries}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Percentage Visited: ${uiState.visitedPercentage}%",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Global Statistik
                    Text(
                        text = "Global Top Countries",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Topp 5 mest besökta
                    if (uiState.topVisitedCountries.isNotEmpty()) {
                        Text(
                            text = "Most Visited:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        uiState.topVisitedCountries.forEachIndexed { index, stat ->
                            Text("${index + 1}. ${stat.countryName}: ${stat.value} visits")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Text("No global visited data yet.")
                        Spacer(modifier = Modifier.height(16.dp))
                    }


                    // Topp 5 högst betygsatta
                    if (uiState.topRatedCountries.isNotEmpty()) {
                        Text(
                            text = "Highest Rated:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        uiState.topRatedCountries.forEachIndexed { index, stat ->
                            Text("${index + 1}. ${stat.countryName}: ${stat.value} avg rating")
                        }
                    } else {
                        Text("No global rating data yet.")
                    }

                    Spacer(modifier = Modifier.weight(1f)) // Skjut upp resten

                    // Valfri Logut knapp här om du vill
                    // Button(onClick = onLogoutClick) { Text("Logout") }
                }
            }
        }
    }
}