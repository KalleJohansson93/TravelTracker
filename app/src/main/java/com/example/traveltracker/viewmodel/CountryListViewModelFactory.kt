package com.example.traveltracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.traveltracker.data.CountryRepository
import com.google.firebase.auth.FirebaseAuth

class CountryListViewModelFactory(
    private val countryRepository: CountryRepository,
    private val auth: FirebaseAuth
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CountryListViewModel::class.java)) {
            return CountryListViewModel(countryRepository, auth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}