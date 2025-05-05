package com.example.traveltracker.data

data class StatisticsData(
    val username: String = "Loading...",
    val totalCountries: Int = 0,
    val visitedCountriesCount: Int = 0,
    val visitedPercentage: Int = 0,
    val topVisitedCountries: List<CountryDisplayStat> = emptyList(),
    val topRatedCountries: List<CountryDisplayStat> = emptyList(),
    val topWantedCountries: List<CountryDisplayStat> = emptyList(),
    val topUsersVisited: List<UserStat> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)