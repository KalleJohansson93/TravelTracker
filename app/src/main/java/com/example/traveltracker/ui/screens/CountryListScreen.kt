package com.example.traveltracker.ui.screens

import androidx.compose.foundation.clickable // För att göra Text klickbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Star // Exempel ikon, kräver extended
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign // För Text-justering
import androidx.compose.ui.unit.dp
import com.example.traveltracker.data.Country
import com.example.traveltracker.data.CountryStatus
import com.example.traveltracker.viewmodel.CountryListViewModel
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryListScreen(
    viewModel: CountryListViewModel,
    onLogoutClick: () -> Unit
) {
    val countries by viewModel.countries.collectAsState()
    // Observera statet som håller reda på vilket land som ska betygsättas
    val searchQuery by viewModel.searchQuery.collectAsState()
    val countryToRate by viewModel.countryToRateFlow.collectAsState()

    // Visa betygsdialogen om countryToRate inte är null
    if (countryToRate != null) {
        RatingDialog(
            countryCode = countryToRate!!, // ! eftersom vi vet att den inte är null här
            onRateSelected = { rating -> viewModel.updateCountryRating(countryToRate!!, rating) },
            onDismiss = { viewModel.cancelRating() }
        )
    }

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
        // *** ÄNDRA HÄR: Använd en Column för att lägga sökrutan ovanför listan ***
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Applicera innerPadding på Column
                .padding(horizontal = 16.dp) // Lägg till horisontell padding för hela innehållet
        ) {
            // *** NYTT: SÖKRUTA ***
            OutlinedTextField(
                value = searchQuery, // Använd söktexten från ViewModel
                onValueChange = { query -> viewModel.updateSearchQuery(query) }, // Uppdatera ViewModel vid ändring
                label = { Text("Search Countries") },
                singleLine = true, // En rad text
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp) // Lite marginal ovan/under sökrutan
            )

            // Lägg till en Spacer mellan sökrutan och listan om du vill
            // Spacer(modifier = Modifier.height(8.dp))

            // LazyColumn för landlistan (nu filtrerad)
            if (countries.isNotEmpty()) {
                Log.d("CountryListScreen", "Showing country list with ${countries.size} items")
                LazyColumn(
                    modifier = Modifier.fillMaxSize() // Fyll resten av utrymmet i Column
                ) {
                    items(countries, key = { country -> country.code }) { country ->
                        CountryListItem(
                            country = country,
                            onStatusChanged = { newStatus ->
                                viewModel.updateCountryStatus(country.code, newStatus)
                            },
                            onRateClick = { countryCode -> viewModel.onRateClick(countryCode) }
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            } else {
                Text(
                    text = if (searchQuery.isNotBlank()) "No countries found matching your search." else "Loading countries or no countries found...",
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp) // Justera alignment
                )
                Log.d("CountryListScreen", "Showing empty/loading/no results message")
            }
        }
    }
}

fun String.toEmojiFlag(): String {
    if (this.length != 2) {
        return this
    }

    val countryCodeCaps = this.uppercase()
    val firstChar = countryCodeCaps[0]
    val secondChar = countryCodeCaps[1]

    val unicodeOffset = 0x1F1E6
    val firstEmoji = String(Character.toChars(firstChar.code - 'A'.code + unicodeOffset))
    val secondEmoji = String(Character.toChars(secondChar.code - 'A'.code + unicodeOffset))

    return firstEmoji + secondEmoji
}

// *** SEPARAT COMPOSABLE FÖR ETT LISTITEM ***
@Composable
fun CountryListItem(
    country: Country,
    onStatusChanged: (CountryStatus) -> Unit,
    onRateClick: (String) -> Unit // *** NY LAMBDA FÖR BETYG KLICK ***
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row( // Använd en Rad för namn och betygsknapp/text
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // Sprid ut Name och Rating-yta
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${country.code.toEmojiFlag()} ${country.name}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f) // Låt namnet ta upp utrymme
            )
            // *** BETYG AV-SNITT ***
            Row( // Rad för Betygs-ikon/text och klickbar yta
                modifier = Modifier.padding(start = 8.dp), // Lite marginal
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Valfritt: Visa en stjärnikon eller liknande
                // Icon(Icons.Default.Star, contentDescription = "Rating")

                // Visa nuvarande betyg eller "Rate" om inget betyg
                Text(
                    text = country.userRating?.toString() ?: "Rate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (country.userRating != null) MaterialTheme.colorScheme.primary else LocalContentColor.current, // Annan färg om betygsatt
                    textAlign = TextAlign.End, // Justera texten till höger
                    modifier = Modifier
                        .clickable { onRateClick(country.code) } // *** GÖR KLICKBAR ***
                        .padding(horizontal = 4.dp) // Lägg till klick-yta padding
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row( // Rad för RadioButtons (oförändrad)
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


// *** SEPARAT COMPOSABLE FÖR BETYGSDIALOGEN (1-10) ***
@Composable
fun RatingDialog(
    countryCode: String, // Landkoden som betygsätts
    onRateSelected: (Int) -> Unit, // Lambda när ett betyg väljs
    onDismiss: () -> Unit // Lambda när dialogen stängs (avbryt)
) {
    AlertDialog(
        onDismissRequest = onDismiss, // Stäng dialogen vid klick utanför eller bakåtknapp
        title = {
            // Kan visa landnamnet här, du behöver slå upp det baserat på countryCode
            // Access till landlistan eller skicka in namnet till dialogen
            Text("Rate Country") // Enklast att visa generisk titel nu
        },
        text = {
            // Lista med betyg 1-10 i en kolumn
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                // Skapa 10 rader, en för varje betyg
                (1..10).forEach { rating ->
                    Text(
                        text = rating.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRateSelected(rating) } // Kalla lambda med valt betyg
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    if (rating < 10) {
                        Divider() // Avdelare mellan betyg
                    }
                }
            }
        },
        confirmButton = {
            // En tom confirmButton eller ta bort den om du bara vill klicka på betyget
            // TextButton(onClick = onDismiss) { Text("Cancel") } // Eller en avbryt-knapp
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}