package com.example.traveltracker.data

data class Country(
    val code: String, // ISO 3166-1 alpha-2 kod
    val name: String,
    val userStatus: CountryStatus = CountryStatus.NOT_VISITED, // Standardvärde
    val userRating: Int? = null // Null om inte betygsatt
    // Ev. fler statiska fält från StaticCountry
)

enum class CountryStatus {
    VISITED, WANT_TO_VISIT, NOT_VISITED
}