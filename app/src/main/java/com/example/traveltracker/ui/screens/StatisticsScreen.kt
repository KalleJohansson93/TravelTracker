package com.example.traveltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.traveltracker.viewmodel.StatisticsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onLogoutClick: () -> Unit
) {
    val uiState by viewModel.statisticsUiState.collectAsState()

    Scaffold(
        topBar = {
            // ... (TopAppBar)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.isLoading || uiState.errorMessage != null) {
                item {
                    Column(
                        modifier = Modifier.fillParentMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when {
                            uiState.isLoading -> {
                                CircularProgressIndicator()
                                Text("Loading statistics...")
                            }
                            uiState.errorMessage != null -> {
                                Text(
                                    text = "Error: ${uiState.errorMessage}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            } else {

                item {
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
                    Text(
                        text = "Global Top Countries",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }


                // Topp 5 mest besökta länder
                item {
                    if (uiState.topVisitedCountries.isNotEmpty()) {
                        Text(
                            text = "Most Visited Countries:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    } else {
                        Text("No global visited data yet.")
                    }
                }
                items(uiState.topVisitedCountries) { stat ->
                    Text("${uiState.topVisitedCountries.indexOf(stat) + 1}. ${stat.countryName}: ${stat.value} visits")
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }


                // Topp 5 mest "vill besöka" länder
                item {
                    if (uiState.topWantedCountries.isNotEmpty()) {
                        Text(
                            text = "Most Wanted to Visit Countries:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    } else {
                        Text("No global wanted data yet.")
                    }
                }
                items(uiState.topWantedCountries) { stat ->
                    Text("${uiState.topWantedCountries.indexOf(stat) + 1}. ${stat.countryName}: ${stat.value} wants")
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }


                // Topp 5 högst betygsatta länder
                item {
                    if (uiState.topRatedCountries.isNotEmpty()) {
                        Text(
                            text = "Highest Rated Countries:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    } else {
                        Text("No global rating data yet.")
                    }
                }
                items(uiState.topRatedCountries) { stat ->
                    Text("${uiState.topRatedCountries.indexOf(stat) + 1}. ${stat.countryName}: ${stat.value} avg rating")
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }


                // *** NYTT: Topp 5 användare med flest besökta länder ***
                item {
                    if (uiState.topUsersVisited.isNotEmpty()) {
                        Text(
                            text = "Users with Most Visited Countries:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    } else {
                        Text("No top user data yet (Calculated daily).")
                    }
                }
                items(uiState.topUsersVisited) { userStat ->
                    Text("${uiState.topUsersVisited.indexOf(userStat) + 1}. ${userStat.username}: ${userStat.count} countries") // *** Visa username och count ***
                }
            }
        }
    }
}