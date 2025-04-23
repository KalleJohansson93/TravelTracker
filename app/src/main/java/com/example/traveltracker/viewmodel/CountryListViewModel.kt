package com.example.traveltracker.viewmodel

import CountryRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltracker.data.Country // Din kombinerade Country-klass
import com.example.traveltracker.data.CountryStatus
import com.google.firebase.auth.FirebaseAuth // Behövs för utloggning
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CountryListViewModel(
    private val countryRepository: CountryRepository, // Injicera repository
    private val auth: FirebaseAuth // Injicera FirebaseAuth
) : ViewModel() {

    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries.asStateFlow()


    init {
        // Starta en coroutine för att observera länderna från repository
        viewModelScope.launch {
            // Eftersom repositoryn kombinerar lokal data med en Flow från Firestore,
            // kommer detta flöde att skicka ut nya listor varje gång användardatan i Firestore
            // uppdateras (även via offline cache).
            countryRepository.getCountriesWithUserStatus()
                .catch { e ->
                    // Hantera fel från Firestore-läsningen
                    // _error.value = e.message
                }
                .collect { combinedCountryList ->
                    _countries.value = combinedCountryList
                    // _isLoading.value = false // Laddningen klar när första datan kommer
                }
        }
    }

    fun updateCountryStatus(countryCode: String, newStatus: CountryStatus) {
        viewModelScope.launch {
            try {
                // Anropa repositoryn för att uppdatera status i Firestore
                countryRepository.updateCountryStatus(countryCode, newStatus)
                // Repositoryn skriver till Firestore, som sedan triggar Flowen i init-blocket
                // att skicka ut den uppdaterade listan, som UI:et observerar.
            } catch (e: Exception) {
                // Hantera fel vid uppdatering
                // _error.value = "Failed to update status: ${e.message}"
            }
        }
    }

    fun logout() {
        auth.signOut()
        // ViewModel ska inte hantera navigering, men den kan meddela UI via State/Event
        // eller så hanterar UI LoggedInContent direkt att användaren är utloggad via Auth State.
    }

    // Implementera funktioner för att uppdatera betyg
    // fun updateCountryRating(countryCode: String, rating: Int?) { ... }
}