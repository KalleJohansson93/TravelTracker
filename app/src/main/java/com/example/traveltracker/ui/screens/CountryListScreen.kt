package com.example.traveltracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Kommentera ut om du skickar ViewModel via navigation
import com.example.traveltracker.data.Country
import com.example.traveltracker.data.CountryStatus
import com.example.traveltracker.viewmodel.CountryListViewModel
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryListScreen(
    // Vi tar emot ViewModel som en parameter nu för att skopas av LoggedInContent
    viewModel: CountryListViewModel, // Ingen default viewModel() här längre
    onLogoutClick: () -> Unit // Lambda för utloggning
) {
    val countries by viewModel.countries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Countries") },
                actions = {
                    // Lägg till en utloggningsikon/knapp i TopAppBar
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    Log.d("CountryListScreen", "Showing loading indicator")
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                    Log.e("CountryListScreen", "Showing error: $error")
                }
                countries.isNotEmpty() -> {
                    Log.d("CountryListScreen", "Showing country list with ${countries.size} items")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(countries, key = { country -> country.name }) { country ->
                            CountryListItem(
                                country = country,
                                onStatusChanged = { newStatus ->
                                    viewModel.updateCountryStatus(country.name, newStatus)
                                }
                            )
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
                else -> {
                    Text(
                        text = "No countries found or you might need to log in.",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                    Log.d("CountryListScreen", "Showing empty list message")
                }
            }
        }
    }
}

// CountryListItem Composable förblir oförändrad
@Composable
fun CountryListItem(country: Country, onStatusChanged: (CountryStatus) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = country.name,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = country.userStatus == CountryStatus.VISITED,
                    onClick = { onStatusChanged(CountryStatus.VISITED) }
                )
                Text("Been")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = country.userStatus == CountryStatus.WANT_TO_VISIT,
                    onClick = { onStatusChanged(CountryStatus.WANT_TO_VISIT) }
                )
                Text("Want")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = country.userStatus == CountryStatus.NOT_VISITED,
                    onClick = { onStatusChanged(CountryStatus.NOT_VISITED) }
                )
                Text("Not Visited")
            }
        }
    }
}