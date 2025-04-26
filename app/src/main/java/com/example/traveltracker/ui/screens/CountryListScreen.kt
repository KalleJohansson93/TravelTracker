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
import com.example.traveltracker.data.Country // Din kombinerade Country-klass
import com.example.traveltracker.data.CountryStatus // Din Enum
import com.example.traveltracker.viewmodel.CountryListViewModel
import android.util.Log // Behåll loggning för debugging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryListScreen(
    viewModel: CountryListViewModel,
    onLogoutClick: () -> Unit
) {
    // Observera staten från ViewModel
    val countries by viewModel.countries.collectAsState()
    // Behåll error state för att visa meddelanden om t.ex. Firestore-uppdateringar misslyckas
    // val error by viewModel.error.collectAsState() // Antag att du har en _error StateFlow i ViewModel

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Countries") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            if (countries.isNotEmpty()) {
                Log.d("CountryListScreen", "Showing country list with ${countries.size} items")
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(countries, key = { country -> country.code }) { country ->
                        CountryListItem(
                            country = country,
                            onStatusChanged = { newStatus ->
                                // Anropa ViewModel för att uppdatera status, skicka landkoden
                                viewModel.updateCountryStatus(country.code, newStatus)
                            }
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            } else {

                Text(
                    text = "Loading countries or no countries found...", // Bättre text
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
                Log.d("CountryListScreen", "Showing empty/loading message")
            }

            // Exempel: Visa en CircularProgressIndicator endast om ViewModel indikerar en PÅGÅENDE operation
            // som tar tid UTÖVER den initiala laddningen/synkningen.
            // T.ex. om du har en separat flagga i ViewModel för "isUpdatingStatus"
            // val isUpdatingStatus by viewModel.isUpdatingStatus.collectAsState()
            // if (isUpdatingStatus) {
            //     CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
            // }
        }
    }
}

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