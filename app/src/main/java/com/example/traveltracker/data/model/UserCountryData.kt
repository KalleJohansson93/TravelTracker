package com.example.traveltracker.data.model

data class UserCountryData(
    val status: String = CountryStatus.NOT_VISITED.name,
    val rating: Int? = null
)