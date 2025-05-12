package com.example.traveltracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.traveltracker.data.model.Country
import com.example.traveltracker.data.model.CountryStatus
import com.example.traveltracker.viewmodel.CountryListViewModel
import android.util.Log

@Composable
fun CountryListScreen(
    viewModel: CountryListViewModel,
) {
    val countries by viewModel.countries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val countryToRate by viewModel.countryToRateFlow.collectAsState()

    if (countryToRate != null) {
        RatingDialog(
            countryCode = countryToRate!!,
            onRateSelected = { rating -> viewModel.updateCountryRating(countryToRate!!, rating) },
            onDismiss = { viewModel.cancelRating() }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query -> viewModel.updateSearchQuery(query) },
            label = { Text("Search Countries") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
        )

        if (countries.isNotEmpty()) {
            Log.d("CountryListScreen", "Showing country list with ${countries.size} items")
            LazyColumn(
                modifier = Modifier.fillMaxSize()
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
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )
            Log.d("CountryListScreen", "Showing empty/loading/no results message")
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

@Composable
fun CountryListItem(
    country: Country,
    onStatusChanged: (CountryStatus) -> Unit,
    onRateClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${country.code.toEmojiFlag()} ${country.name}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier.padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = country.userRating?.toString() ?: "Rate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (country.userRating != null) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .clickable { onRateClick(country.code) }
                        .padding(horizontal = 4.dp)
                )
            }
        }
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

@Composable
fun RatingDialog(
    countryCode: String,
    onRateSelected: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rate Country")
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Bad",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                (1..10).forEach { rating ->
                    Text(
                        text = rating.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRateSelected(rating) }
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )

                    if (rating < 10) {
                        Divider()
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Best",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { onRateSelected(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove Rating", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}