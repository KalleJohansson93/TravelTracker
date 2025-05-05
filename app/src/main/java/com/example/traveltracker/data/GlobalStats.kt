package com.example.traveltracker.data

data class GlobalStats(
    val mostVisited: List<CountryStat> = emptyList(), // Lista av {code, count}
    val highestRated: List<CountryStat> = emptyList(), // Lista av {code, averageRating}
    val mostWanted: List<CountryStat> = emptyList(),
    val topUsersVisited: List<UserStat> = emptyList()
)