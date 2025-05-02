package com.example.traveltracker.data

data class CountryDisplayStat(
    val countryName: String = "Unknown",
    val value: String = "" // Antal besökta eller betyg (formaterat som sträng)
)