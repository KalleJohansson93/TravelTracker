package com.example.traveltracker.data

import com.example.traveltracker.data.model.CountryStatus
import com.example.traveltracker.data.model.UserCountryData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await



class FirestoreUserCountryDataSource(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private fun getUserCountriesCollectionRef() =
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).collection("userCountries")
        }

    fun getUserCountryData(): Flow<Map<String, UserCountryData>> {
        val collectionRef = getUserCountriesCollectionRef()
        return collectionRef?.snapshots()?.map { querySnapshot ->
            querySnapshot.documents.associate { document ->
                document.id to (document.toObject<UserCountryData>() ?: UserCountryData())
            }
        } ?: kotlinx.coroutines.flow.flowOf(emptyMap())
    }

    suspend fun updateCountryStatus(countryCode: String, status: CountryStatus) {
        val userId = auth.currentUser?.uid ?: return

        val countryDocRef = firestore.collection("users").document(userId).collection("userCountries").document(countryCode)

        try {
            countryDocRef.set(
                mapOf(
                    "status" to status.name,
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreDataSource", "Error updating status for $countryCode: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateCountryRating(countryCode: String, rating: Int?) {
        val docRef = getUserCountriesCollectionRef()?.document(countryCode) ?: return

        docRef.set(mapOf("rating" to rating), SetOptions.merge()).await()
        android.util.Log.d("FirestoreWrite", "Rating update successful for $countryCode: $rating")
    }
}