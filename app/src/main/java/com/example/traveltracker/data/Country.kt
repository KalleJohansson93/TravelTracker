package com.example.traveltracker.data

data class Country(
    val name: String,
    var status: CountryStatus = CountryStatus.NOT_VISITED
)

enum class CountryStatus {
    VISITED,
    WANT_TO_VISIT,
    NOT_VISITED
}