package com.example.traveltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Om du behöver en ViewModel här

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    // Lägg till ViewModel här när du skapar den
    // viewModel: StatisticsViewModel = viewModel(),
    onLogoutClick: () -> Unit // Lambda för utloggning
) {
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
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Statistics will go here!",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.padding(16.dp))
            // Lägg till dina statistik-element här (grafer, summor, etc.)
            // Knapp för att trigga logout (kan finnas även i TopAppBar)
            // Button(onClick = onLogoutClick) {
            //     Text("Logout (Example Button)")
            // }
        }
    }
}