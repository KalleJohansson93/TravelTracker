package com.example.traveltracker.data

data class StatisticsData(
    val username: String = "Loading...", // Användarnamn
    val totalCountries: Int = 0, // Totalt antal länder från lokal JSON
    val visitedCountriesCount: Int = 0, // Antal besökta länder för denna användare
    val visitedPercentage: Int = 0, // Procent besökta länder (0-100)
    val topVisitedCountries: List<CountryDisplayStat> = emptyList(), // Topp 5 globalt (med landnamn)
    val topRatedCountries: List<CountryDisplayStat> = emptyList(), // Topp 5 globalt (med landnamn)
    val topWantedCountries: List<CountryDisplayStat> = emptyList(),
    val topUsersVisited: List<UserStat> = emptyList(),
    val isLoading: Boolean = true, // Laddningsindikator för statistikdata
    val errorMessage: String? = null // Felmeddelande
)