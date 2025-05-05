package com.example.traveltracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class CountryRepository(
    private val localDataSource: LocalCountryDataSource,
    private val firestoreDataSource: FirestoreUserCountryDataSource
) {

    fun getCountriesWithUserStatus(): Flow<List<Country>> {
        val staticCountries = localDataSource.getStaticCountries()

        if (staticCountries.isEmpty()) {
            return flowOf(emptyList())
        }

        return firestoreDataSource.getUserCountryData().map { userCountryDataMap ->
            staticCountries.map { staticCountry ->
                val userCountryData = userCountryDataMap[staticCountry.code]
                Country(
                    code = staticCountry.code,
                    name = staticCountry.name,
                    //continent = staticCountry.continent,
                    userStatus = userCountryData?.status?.let { statusString ->
                        try { CountryStatus.valueOf(statusString) } catch (e: IllegalArgumentException) { CountryStatus.NOT_VISITED }
                    } ?: CountryStatus.NOT_VISITED,
                    userRating = userCountryData?.rating
                )
            }
        }
    }

    suspend fun updateCountryStatus(countryCode: String, status: CountryStatus) {
        firestoreDataSource.updateCountryStatus(countryCode, status)
    }

    suspend fun updateCountryRating(countryCode: String, rating: Int?) {
        firestoreDataSource.updateCountryRating(countryCode, rating)
    }
}