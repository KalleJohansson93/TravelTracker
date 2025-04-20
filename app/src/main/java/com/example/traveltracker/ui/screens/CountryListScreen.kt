package com.example.traveltracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveltracker.data.CountryStatus
import com.example.traveltracker.viewmodel.CountryListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryListScreen(viewModel: CountryListViewModel = viewModel()) {
    val countries by viewModel.countries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Välj länder") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            countries.forEach { country ->
                CountryListItem(country = country, onStatusChanged = { newStatus ->
                    viewModel.updateCountryStatus(country.name, newStatus)
                })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CountryListItem(country: com.example.traveltracker.data.Country, onStatusChanged: (CountryStatus) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = country.name)
        Row {
            RadioButton(
                selected = country.status == CountryStatus.VISITED,
                onClick = { onStatusChanged(CountryStatus.VISITED) }
            )
            Text("Besökt")
            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(
                selected = country.status == CountryStatus.WANT_TO_VISIT,
                onClick = { onStatusChanged(CountryStatus.WANT_TO_VISIT) }
            )
            Text("Vill besöka")
            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(
                selected = country.status == CountryStatus.NOT_VISITED,
                onClick = { onStatusChanged(CountryStatus.NOT_VISITED) }
            )
            Text("Ej besökt")
        }
    }
}