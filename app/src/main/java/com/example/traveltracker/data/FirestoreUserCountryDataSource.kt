package com.example.traveltracker.data

import com.example.traveltracker.data.CountryStatus // Din Enum
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.snapshots // KTX extension for snapshots
import com.google.firebase.firestore.ktx.toObject // KTX extension for toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await



class FirestoreUserCountryDataSource(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    // Referens till den nuvarande användarens under-samling, null om ingen är inloggad
    private fun getUserCountriesCollectionRef() =
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).collection("userCountries")
        }

    // Hämta all användares landdata som ett Flow
    fun getUserCountryData(): Flow<Map<String, UserCountryData>> {
        val collectionRef = getUserCountriesCollectionRef()
        // Om collectionRef är null (ingen inloggad), returnera ett Flow med en tom mapp
        return collectionRef?.snapshots()?.map { querySnapshot ->
            querySnapshot.documents.associate { document ->
                // Mappa dokument-ID (landkoden) till UserCountryData-objekt
                document.id to (document.toObject<UserCountryData>() ?: UserCountryData())
            }
        } ?: kotlinx.coroutines.flow.flowOf(emptyMap()) // Returnera tomt flow om ingen användare
    }

    // Uppdatera status för ett specifikt land
    suspend fun updateCountryStatus(countryCode: String, status: CountryStatus) {
        android.util.Log.e("FirestoreWrite", "User not logged in. Cannot update status for $countryCode")
        val docRef = getUserCountriesCollectionRef()?.document(countryCode) ?: return // Ingen inloggad eller fel ref
        android.util.Log.d("FirestoreWrite", "User logged in. Attempting to update status for $countryCode")

        // Använd SetOptions.merge() för att inte skriva över andra fält som t.ex. rating
        android.util.Log.d("FirestoreWrite", "Status update successful for $countryCode")
        docRef.set(mapOf("status" to status.name), SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("FirestoreWrite", "Status update successful for $countryCode")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirestoreWrite", "Status update failed for $countryCode: ${e.message}", e)
            }
            .await() // Behåll await för att suspendera coroutinen
    }

    // Uppdatera betyg för ett specifikt land (implementera liknande)
    suspend fun updateCountryRating(countryCode: String, rating: Int?) {
        val docRef = getUserCountriesCollectionRef()?.document(countryCode) ?: return // Ingen inloggad eller fel ref

        docRef.set(mapOf("rating" to rating), SetOptions.merge()).await()
    }
}