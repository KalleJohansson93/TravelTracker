// package com.example.traveltracker.data

import com.example.traveltracker.data.Country
import com.example.traveltracker.data.CountryStatus // Din Enum
import com.example.traveltracker.data.FirestoreUserCountryDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine // Använd combine istället för map på Firestore flowen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

// Din kombinerade Country-klass (som UI använder)
// data class Country(...)
// enum class CountryStatus { ... }


class CountryRepository(
    private val localDataSource: LocalCountryDataSource,
    private val firestoreDataSource: FirestoreUserCountryDataSource
) {

    // Hämta listan av länder, kombinerad med användarens status från Firestore
    fun getCountriesWithUserStatus(): Flow<List<Country>> {
        val staticCountries = localDataSource.getStaticCountries() // Hämta statisk data en gång

        if (staticCountries.isEmpty()) {
            // Om den statiska listan är tom, kan vi inte visa något, returnera ett tomt flow
            return flowOf(emptyList())
        }

        // Kombinera den statiska listan (som är konstant) med Flowet från Firestore
        // combine kommer att skicka ut en ny lista varje gång Firestore-datan ändras
        return firestoreDataSource.getUserCountryData().map { userCountryDataMap ->
            // Gå igenom den statiska listan och berika med användarens data
            staticCountries.map { staticCountry ->
                val userCountryData = userCountryDataMap[staticCountry.code] // Slå upp data med landkoden
                Country(
                    code = staticCountry.code,
                    name = staticCountry.name,
                    userStatus = userCountryData?.status?.let { statusString ->
                        try { CountryStatus.valueOf(statusString) } catch (e: IllegalArgumentException) { CountryStatus.NOT_VISITED }
                    } ?: CountryStatus.NOT_VISITED, // Default Not Visited om ingen data finns
                    userRating = userCountryData?.rating
                    // Kopiera över andra statiska fält om de finns i Country
                )
            }
        }
    }

    // Anropa Firestore-datakällan för att uppdatera status
    suspend fun updateCountryStatus(countryCode: String, status: CountryStatus) {
        firestoreDataSource.updateCountryStatus(countryCode, status)
    }

    // Anropa Firestore-datakällan för att uppdatera betyg
    suspend fun updateCountryRating(countryCode: String, rating: Int?) {
        firestoreDataSource.updateCountryRating(countryCode, rating)
    }
}