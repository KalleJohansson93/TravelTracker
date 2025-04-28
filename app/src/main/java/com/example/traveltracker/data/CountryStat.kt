package com.example.traveltracker.data

data class CountryStat(
    val countryCode: String = "",
    val count: Int? = null, // För mostVisited
    val averageRating: Double? = null // För highestRated
)