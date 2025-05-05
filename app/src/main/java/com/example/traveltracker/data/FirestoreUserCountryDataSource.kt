package com.example.traveltracker.data

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
        val docRef = getUserCountriesCollectionRef()?.document(countryCode) ?: return

        docRef.set(mapOf("status" to status.name), SetOptions.merge())
            .addOnSuccessListener {
            }
            .addOnFailureListener { e ->
            }
            .await()
    }

    suspend fun updateCountryRating(countryCode: String, rating: Int?) {
        val docRef = getUserCountriesCollectionRef()?.document(countryCode) ?: return

        docRef.set(mapOf("rating" to rating), SetOptions.merge()).await()
        android.util.Log.d("FirestoreWrite", "Rating update successful for $countryCode: $rating")
    }
}