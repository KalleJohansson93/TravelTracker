package com.example.traveltracker.data

import com.google.firebase.firestore.DocumentId // Används om dokument-ID är landkoden

data class UserCountryData(
    //@DocumentId val countryCode: String = "", // Mappar till dokument-ID (landkoden)
    val status: String = CountryStatus.NOT_VISITED.name, // T.ex. spara som String
    val rating: Int? = null // Null om inte betygsatt
    // Lägg till tidsstämpel om du vill
)