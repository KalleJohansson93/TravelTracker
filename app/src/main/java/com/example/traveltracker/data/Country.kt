package com.example.traveltracker.data

import com.google.firebase.firestore.DocumentId

data class Country(
    @DocumentId // Viktigt: Mappar Firestore dokument-ID till detta fält
    val name: String = "",
    val continent: String? = null, // Kan vara null om den inte finns i DB

    // Statistikfält (läses från DB, men uppdateras via Cloud Functions)
    val averageRating: Double = 0.0,
    val ratingCount: Long = 0, // Firestore sparar heltal som Long
    val visitCount: Long = 0,

    // Detta fält finns INTE i 'countries'-samlingen direkt.
    // Det läggs till i ViewModel genom att kombinera med UserCountryData.
    var userStatus: CountryStatus = CountryStatus.NOT_VISITED,
    var userRating: Int? = null // Användarens specifika betyg
)

enum class CountryStatus {
    VISITED,
    WANT_TO_VISIT,
    NOT_VISITED
}