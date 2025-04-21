package com.example.traveltracker.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserCountryData(
    val userId: String = "",
    val country: String = "", // Matchar Country.code
    val status: String? = null, // "visited", "want_to_visit", eller null
    val rating: Long? = null, // Firestore sparar heltal som Long, kan vara null

    @ServerTimestamp // Firestore sätter automatiskt tid vid skrivning
    val updatedAt: Date? = null
) {
    // Helper för att mappa Firestore String till Enum
    fun mapToCountryStatus(): CountryStatus {
        return when (status) {
            "visited" -> CountryStatus.VISITED
            "want_to_visit" -> CountryStatus.WANT_TO_VISIT
            else -> CountryStatus.NOT_VISITED
        }
    }

    // Helper för att mappa Firestore Long till Int? för betyg
    fun mapToUserRating(): Int? {
        return rating?.toInt()
    }
}

// Helper för att mappa Enum till Firestore String (används vid skrivning)
fun CountryStatus.toFirestoreString(): String? {
    return when (this) {
        CountryStatus.VISITED -> "visited"
        CountryStatus.WANT_TO_VISIT -> "want_to_visit"
        CountryStatus.NOT_VISITED -> null // Representerar "ingen status satt" eller "inte besökt"
    }
}