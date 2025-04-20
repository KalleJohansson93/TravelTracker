package com.example.traveltracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveltracker.data.Country
import com.example.traveltracker.data.CountryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CountryListViewModel : ViewModel() {

    private val _countries = MutableStateFlow(listOf<Country>())
    val countries: StateFlow<List<Country>> = _countries

    init {
        // Lägg till en initial lista med länder (kan ersättas med data från databasen senare)
        val initialCountries = listOf(
            Country("Sverige"),
            Country("Norge"),
            Country("Danmark"),
            Country("Finland"),
            Country("Island"),
            Country("Frankrike"),
            Country("Spanien"),
            Country("Italien"),
            Country("Grekland"),
            Country("Japan"),
            Country("USA"),
            Country("Kanada")
            // Lägg till fler länder här
        )
        _countries.value = initialCountries
    }

    fun updateCountryStatus(countryName: String, newStatus: CountryStatus) {
        _countries.update { list ->
            list.map { country ->
                if (country.name == countryName) {
                    country.copy(status = newStatus)
                } else {
                    country
                }
            }
        }
    }
}