package com.example.traveltracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltracker.data.model.Country
import com.example.traveltracker.data.CountryRepository
import com.example.traveltracker.data.model.CountryStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CountryListViewModel(
    private val countryRepository: CountryRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _allCountries = MutableStateFlow<List<Country>>(emptyList())

    private val _countryToRateFlow = MutableStateFlow<String?>(null)
    val countryToRateFlow: StateFlow<String?> = _countryToRateFlow.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val countries: StateFlow<List<Country>> = combine(
        _allCountries,
        _searchQuery
    ) { allCountries, query ->
        if (query.isBlank()) {
            allCountries
        } else {
            allCountries.filter { country ->
                country.name.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )



    init {
        viewModelScope.launch {
            countryRepository.getCountriesWithUserStatus()
                .catch { e ->
                    // _error.value = e.message
                }
                .collect { combinedCountryList ->
                    _allCountries.value = combinedCountryList
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCountryStatus(countryCode: String, newStatus: CountryStatus) {
        viewModelScope.launch {
            try {
                countryRepository.updateCountryStatus(countryCode, newStatus)
            } catch (e: Exception) {
                // _error.value = "Failed to update status: ${e.message}"
            }
        }
    }

    fun onRateClick(countryCode: String) {
        _countryToRateFlow.value = countryCode
    }

    fun updateCountryRating(countryCode: String, rating: Int?) {
        viewModelScope.launch {
            try {
                countryRepository.updateCountryRating(countryCode, rating)
                _countryToRateFlow.value = null
            } catch (e: Exception) {
                _countryToRateFlow.value = null
            }
        }
    }

    fun cancelRating() {
        _countryToRateFlow.value = null
    }
}