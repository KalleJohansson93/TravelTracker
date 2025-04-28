package com.example.traveltracker.viewmodel

import CountryRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    // *** ÄNDRA DENNA TILL MutableStateFlow OCH StateFlow ***
    // Den privata, mutable versionen
    private val _countryToRateFlow = MutableStateFlow<String?>(null)
    // Den publika, read-only versionen som UI observerar
    val countryToRateFlow: StateFlow<String?> = _countryToRateFlow.asStateFlow()



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
            } catch (e: Exception) {
                // Hantera fel vid uppdatering
                // _error.value = "Failed to update status: ${e.message}"
            }
        }
    }

    // *** FUNKTION FÖR ATT INDIKERA ATT ANVÄNDAREN VILL BETYGSÄTTA ETT LAND ***
    fun onRateClick(countryCode: String) {
        // Sätt statet till landkoden för att visa betygsdialogen för det landet
        _countryToRateFlow.value = countryCode
    }

    // *** FUNKTION FÖR ATT UPPDATERA ETT LANDS BETYG ***
    fun updateCountryRating(countryCode: String, rating: Int) {
        viewModelScope.launch {
            try {
                // Kalla repositoryn för att spara betyget
                countryRepository.updateCountryRating(countryCode, rating)
                // Nollställ statet för att dölja betygsdialogen efter att betyget har skickats
                _countryToRateFlow.value = null
            } catch (e: Exception) {
                // Hantera fel
                // Nollställ statet även vid fel för att inte blockera UI
                _countryToRateFlow.value = null
            }
        }
    }

    // *** FUNKTION FÖR ATT AVBRYTA BETYGSÄTTNING ***
    fun cancelRating() {
        _countryToRateFlow.value = null // Nollställ statet för att dölja dialogen
    }
}