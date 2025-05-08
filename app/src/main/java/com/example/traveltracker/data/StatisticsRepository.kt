package com.example.traveltracker.data

import com.example.traveltracker.data.model.CountryDisplayStat
import com.example.traveltracker.data.model.CountryStatus
import com.example.traveltracker.data.model.StatisticsData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

class StatisticsRepository(
    private val localDataSource: LocalCountryDataSource,
    private val firestoreUserCountryDataSource: FirestoreUserCountryDataSource,
    private val firestoreUserDataDataSource: FirestoreUserDataDataSource,
    private val firestoreGlobalStatsDataSource: FirestoreGlobalStatsDataSource
) {

    fun getStatisticsData(): Flow<StatisticsData> {
        val staticCountries = localDataSource.getStaticCountries()
        val totalCountries = staticCountries.size
        val staticCountryMap = staticCountries.associateBy { it.code }

        // Kombinera Flows från alla Firestore Data Sources
        return combine(
            firestoreUserDataDataSource.getUserProfile(),
            firestoreUserCountryDataSource.getUserCountryData(),
            firestoreGlobalStatsDataSource.getGlobalStats()
        ) { userProfile, userCountryDataMap, globalStats ->
            val username = userProfile?.username ?: "Guest"
            val visitedCountriesCount = userCountryDataMap?.count { (_, data) ->
                data.status == CountryStatus.VISITED.name
            } ?: 0
            val wantedCountriesCount = userCountryDataMap?.count { (_, data) ->
                data.status == CountryStatus.WANT_TO_VISIT.name
            } ?: 0

            val visitedPercentage = if (totalCountries > 0) {
                (visitedCountriesCount * 100) / totalCountries
            } else {
                0
            }

            val topVisitedDisplay = globalStats?.mostVisited?.mapNotNull { stat ->
                staticCountryMap[stat.countryCode]?.let { staticCountry ->
                    CountryDisplayStat(
                        countryName = staticCountry.name,
                        value = stat.count?.toString() ?: "N/A"
                    )
                }
            } ?: emptyList()

            val topWantedDisplay = globalStats?.mostWanted?.mapNotNull { stat ->
                staticCountryMap[stat.countryCode]?.let { staticCountry ->
                    CountryDisplayStat(
                        countryName = staticCountry.name,
                        value = stat.count?.toString() ?: "N/A"
                    )
                }
            } ?: emptyList()

            val topRatedDisplay = globalStats?.highestRated?.mapNotNull { stat ->
                staticCountryMap[stat.countryCode]?.let { staticCountry ->
                    CountryDisplayStat(
                        countryName = staticCountry.name,
                        value = stat.averageRating?.let { "%.2f".format(it) } ?: "N/A"
                    )
                }
            } ?: emptyList()

            val topUsersDisplay = globalStats?.topUsersVisited ?: emptyList()

            StatisticsData(
                username = username,
                totalCountries = totalCountries,
                visitedCountriesCount = visitedCountriesCount,
                visitedPercentage = visitedPercentage,
                topVisitedCountries = topVisitedDisplay.take(10),
                topRatedCountries = topRatedDisplay.take(10),
                topWantedCountries = topWantedDisplay.take(10),
                topUsersVisited = topUsersDisplay.take(10),
                isLoading = false,
                errorMessage = null
            )
        }.catch { e ->
            emit(StatisticsData(errorMessage = "Failed to load statistics: ${e.message}"))
        }
    }
}