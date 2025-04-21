package com.example.traveltracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltracker.data.* // Importera alla data-klasser
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log // För loggning

class CountryListViewModel(
    private val repository: CountryRepository = FirebaseCountryRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    companion object {
        private const val TAG = "CountryListViewModel"
    }

    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries

    private val _isLoading = MutableStateFlow(true) // Start as true until first load attempt
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // REMOVE OR COMMENT OUT THE INIT BLOCK
    /*
    init {
        // loadCountries() // Let the UI trigger the initial load via userLoggedIn()
    }
    */

    private fun loadCountries() {
        viewModelScope.launch {
            // Reset state before loading attempt
            _isLoading.value = true
            _error.value = null
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Log.w(TAG, "loadCountries: User not logged in, cannot load data.") // Changed to warning
                _error.value = "You need to be logged in to see the countries."
                _isLoading.value = false
                _countries.value = emptyList()
                return@launch
            }
            Log.d(TAG, "loadCountries: Loading data for user: $userId")

            try {
                repository.getAllCountriesStream()
                    .combine(repository.getUserCountryDataStream(userId)) { allCountries, userSpecificData ->
                        // ... (rest of the combine logic is fine) ...
                        Log.d(TAG, "Combining ${allCountries.size} countries with ${userSpecificData.size} user data entries")
                        val userDataMap = userSpecificData.associateBy { it.country }
                        allCountries.map { country ->
                            val userData = userDataMap[country.name]
                            country.copy(
                                userStatus = userData?.mapToCountryStatus() ?: CountryStatus.NOT_VISITED,
                                userRating = userData?.mapToUserRating()
                            )
                        }.sortedBy { it.name }
                    }
                    .catch { e ->
                        Log.e(TAG, "Error in combined flow", e)
                        _error.value = "Failed to load country data: ${e.message}"
                        _countries.value = emptyList()
                        _isLoading.value = false // Stop loading on error
                    }
                    .collect { combinedList ->
                        Log.d(TAG, "Successfully combined data. Updating UI with ${combinedList.size} items.")
                        _countries.value = combinedList
                        _isLoading.value = false // Data loaded successfully
                        _error.value = null // Clear any previous error
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error launching data loading", e)
                _error.value = "An unexpected error occurred: ${e.message}"
                _isLoading.value = false
                _countries.value = emptyList()
            }
        }
    }

    // updateCountryStatus remains the same

    fun updateCountryStatus(country: String, newStatus: CountryStatus) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "User not logged in, cannot update status.")
                _error.value = "You must be logged in to update status."
                return@launch
            }
            if (country.isEmpty()) {
                Log.w(TAG, "Country is empty, cannot update status.")
                return@launch
            }

            Log.d(TAG, "Updating status for $country to $newStatus for user $userId")
            try {
                // Optimistic update
                _countries.update { currentList ->
                    currentList.map {
                        if (it.name == country) it.copy(userStatus = newStatus) else it
                    }
                }
                repository.updateUserCountryStatus(userId, country, newStatus)
                Log.d(TAG, "Status update request sent for $country.")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating status for $country", e)
                _error.value = "Failed to update status: ${e.message}"
                // Consider reverting optimistic update or reloading data
                // refreshData() // Reload all data on error? Maybe too disruptive.
            }
        }
    }


    fun refreshData() {
        Log.d(TAG, "refreshData: Manual refresh triggered")
        // Don't clear countries immediately, let loadCountries handle it
        // _countries.value = emptyList() // Avoid flicker
        loadCountries() // Just call loadCountries which handles loading state
    }

    fun userLoggedOut() {
        Log.d(TAG, "userLoggedOut: Clearing user-specific data.")
        _countries.value = emptyList()
        _isLoading.value = false // Not loading anymore
        _error.value = null // Clear errors
        // The repository streams might still be active but won't return user data
    }

    fun userLoggedIn() {
        Log.d(TAG, "userLoggedIn: User is logged in, triggering data load/refresh.")
        // Don't reset isLoading here, loadCountries will set it.
        refreshData() // Load/refresh data for the logged-in user
    }

    // updateCountryRating remains the same (implementation needed)
}