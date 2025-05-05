package com.example.traveltracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traveltracker.viewmodel.LoginViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    loginViewModel: LoginViewModel = viewModel() // Använd samma ViewModel för enkelhetens skull
) {
    val uiState by loginViewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val isRegisterEnabled by remember { derivedStateOf { email.isNotBlank() && password.isNotBlank() && password == confirmPassword } }

    LaunchedEffect(uiState.isUserLoggedIn) {
        if (uiState.isUserLoggedIn) {
            onRegisterSuccess()
            loginViewModel.resetState() // Återställ state efter lyckad registrering
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Skapa konto", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-post") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Lösenord") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Bekräfta lösenord") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                loginViewModel.register(email, password, username, onRegisterSuccess)
            },
            enabled = isRegisterEnabled && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isLoading) "Skapar konto..." else "Skapa konto")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text("Har du redan ett konto? Logga in.")
        }
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val errorMessage = uiState.errorMessage ?: ""
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}