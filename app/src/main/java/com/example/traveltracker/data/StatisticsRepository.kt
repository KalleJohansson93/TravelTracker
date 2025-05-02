package com.example.traveltracker.data

import LocalCountryDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine // Viktigt! Använd combine för att kombinera Flows
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map // Behövs fortfarande för transformationer

// ... befintliga classes (Country, CountryStatus, StaticCountry, UserCountryData, FirestoreUserCountryDataSource, LocalCountryDataSource, FirestoreUserDataDataSource, FirestoreGlobalStatsDataSource)

class StatisticsRepository(
    private val localDataSource: LocalCountryDataSource,
    private val firestoreUserCountryDataSource: FirestoreUserCountryDataSource,
    private val firestoreUserDataDataSource: FirestoreUserDataDataSource,
    private val firestoreGlobalStatsDataSource: FirestoreGlobalStatsDataSource
) {

    fun getStatisticsData(): Flow<StatisticsData> {
        // Hämta den statiska landlistan en gång för totala antalet länder
        val staticCountries = localDataSource.getStaticCountries()
        val totalCountries = staticCountries.size
        val staticCountryMap = staticCountries.associateBy { it.code } // Mappa till landkod för snabb uppslagning

        // Kombinera Flows från alla Firestore Data Sources
        return combine(
            firestoreUserDataDataSource.getUserProfile(), // Användarens profil (för username)
            firestoreUserCountryDataSource.getUserCountryData(), // Användarens landdata (för räkna besökta)
            firestoreGlobalStatsDataSource.getGlobalStats() // Global statistik
        ) { userProfile, userCountryDataMap, globalStats ->
            // Detta block körs varje gång NÅGON av de kombinerade Flows skickar ut ny data

            val username = userProfile?.username ?: "Guest" // Default username om ingen profil
            val visitedCountriesCount = userCountryDataMap?.count { (_, data) ->
                data.status == CountryStatus.VISITED.name // Räkna länder med status "VISITED"
            } ?: 0 // Default 0 om ingen userCountryDataMap

            val visitedPercentage = if (totalCountries > 0) {
                (visitedCountriesCount * 100) / totalCountries
            } else {
                0
            }

            // Förbered globala topplistor med landnamn från den statiska listan
            val topVisitedDisplay = globalStats?.mostVisited?.mapNotNull { stat ->
                staticCountryMap[stat.countryCode]?.let { staticCountry ->
                    CountryDisplayStat(
                        countryName = staticCountry.name,
                        value = stat.count?.toString() ?: "N/A" // Visa antalet besökta
                    )
                }
            } ?: emptyList()

            val topRatedDisplay = globalStats?.highestRated?.mapNotNull { stat ->
                staticCountryMap[stat.countryCode]?.let { staticCountry ->
                    CountryDisplayStat(
                        countryName = staticCountry.name,
                        value = stat.averageRating?.let { "%.2f".format(it) } ?: "N/A" // Visa formaterat betyg
                    )
                }
            } ?: emptyList()


            // Skapa StatisticsData objektet
            StatisticsData(
                username = username,
                totalCountries = totalCountries,
                visitedCountriesCount = visitedCountriesCount,
                visitedPercentage = visitedPercentage,
                topVisitedCountries = topVisitedDisplay.take(5), // Ta bara topp 5
                topRatedCountries = topRatedDisplay.take(5), // Ta bara topp 5
                isLoading = false, // Data laddad
                errorMessage = null // Inget fel vid lyckad kombination
            )
        }.catch { e ->
            // Hantera fel som kan uppstå under processen (t.ex. Firestore-fel)
            emit(StatisticsData(errorMessage = "Failed to load statistics: ${e.message}"))
        }
        // Lägg till .onStart { emit(StatisticsData(isLoading = true)) } om du vill visa laddning tills första datan kommer
    }
}