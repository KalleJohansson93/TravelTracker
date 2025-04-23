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
import androidx.compose.ui.graphics.Color // För feltext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryListScreen(
    // Vi tar emot ViewModel som en parameter
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

            // Visa felmeddelande om det finns ett
            // error?.let { errorMessage ->
            //     Text(
            //         text = "Error: $errorMessage",
            //         color = MaterialTheme.colorScheme.error,
            //         modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
            //     )
            // }

            // Vi visar listan om den inte är tom
            if (countries.isNotEmpty()) {
                Log.d("CountryListScreen", "Showing country list with ${countries.size} items")
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Använd landkoden som nyckel istället för namn, den är stabilare
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
                // Visa ett meddelande om listan är tom (t.ex. vid uppstart innan data laddats,
                // eller om JSON-filen inte lästes korrekt, eller om ingen användare är inloggad
                // och repositoryn returnerar tom lista).
                // Med din setup borde den lokala listan laddas snabbt, så detta är mer för fel/tom data
                // Om du vill visa en laddningsindikator HÄR medan Firestore hämtar ANVÄNDAR-data
                // (efter att den statiska listan laddats), kan du lägga till isLoading tillbaka
                // i ViewModel och observera den här. Men för initial visning av en tom lista är detta OK.

                // Om du vill visa en laddningsindikator *specifikt* medan den *initiala användardatan*
                // synkas från Firestore (om cachen är tom), kan du lägga till en isLoading-flagga
                // i din ViewModel som sätts till true i init() och false när den första datan kommer.
                // Sedan lägger du till en when(isLoading) { ... } här.
                // Men om listan blir tom EFTER att data visats (t.ex. utloggning),
                // är detta meddelande mer passande.

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

// CountryListItem... (ingen ändring behövs här, den tar emot Country och lambda)

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