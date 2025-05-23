package com.example.traveltracker.data.model

// Den här klassen kombinerar statisk info och användares data för UI:t
data class Country(
    val code: String, // ISO 3166-1 alpha-2 kod
    val name: String,
    //val continent: String,
    val userStatus: CountryStatus = CountryStatus.NOT_VISITED,
    val userRating: Int? = null
    // Ev. fler statiska fält från StaticCountry
)

enum class CountryStatus {
    VISITED, WANT_TO_VISIT, NOT_VISITED
}