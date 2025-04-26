package com.example.traveltracker.viewmodel

import CountryRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth

class CountryListViewModelFactory(
    private val countryRepository: CountryRepository,
    private val auth: FirebaseAuth
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST") // Ignorera varningen här, vi gör en säker kontroll
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CountryListViewModel::class.java)) {
            // Skapa ViewModel-instansen med dess beroenden
            return CountryListViewModel(countryRepository, auth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}