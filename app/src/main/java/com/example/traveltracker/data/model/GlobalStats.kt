package com.example.traveltracker.data.model

data class GlobalStats(
    val mostVisited: List<CountryStat> = emptyList(),
    val highestRated: List<CountryStat> = emptyList(),
    val mostWanted: List<CountryStat> = emptyList(),
    val topUsersVisited: List<UserStat> = emptyList()
)