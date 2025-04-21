package com.example.traveltracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log // För loggning

// Interface definierar operationerna
interface CountryRepository {
    fun getAllCountriesStream(): Flow<List<Country>>
    fun getUserCountryDataStream(userId: String): Flow<List<UserCountryData>>
    suspend fun updateUserCountryStatus(userId: String, country: String, status: CountryStatus)
    suspend fun updateUserCountryRating(userId: String, country: String, rating: Int?) // För framtida bruk
}

// Implementation som använder Firebase
class FirebaseCountryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(), // Hämta Firestore-instans
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // Hämta Auth-instans
) : CountryRepository {

    companion object {
        private const val TAG = "FirebaseCountryRepo"
        private const val COUNTRIES_COLLECTION = "countries"
        private const val USER_DATA_COLLECTION = "userCountryData"
    }

    // Hämta alla länder som en Flow (uppdateras i realtid)
    override fun getAllCountriesStream(): Flow<List<Country>> = callbackFlow {
        val listenerRegistration = firestore.collection(COUNTRIES_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching countries", error)
                    close(error) // Stäng flow med fel
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val countries = snapshot.toObjects<Country>()
                    Log.d(TAG, "Fetched ${countries.size} countries")
                    trySend(countries).isSuccess // Skicka data till flow
                } else {
                    Log.d(TAG, "Snapshot was null")
                    trySend(emptyList()).isSuccess // Skicka tom lista om snapshot är null
                }
            }
        // Stäng lyssnaren när flowen avslutas
        awaitClose { listenerRegistration.remove() }
    }

    // Hämta en specifik användares land-data som en Flow
    override fun getUserCountryDataStream(userId: String): Flow<List<UserCountryData>> = callbackFlow {
        if (userId.isEmpty()) {
            Log.w(TAG,"User ID is empty, returning empty flow for user data")
            trySend(emptyList()).isSuccess
            close() // Stäng flow om ingen användare är inloggad
            return@callbackFlow
        }
        val listenerRegistration = firestore.collection(USER_DATA_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching user data for $userId", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val userData = snapshot.toObjects<UserCountryData>()
                    Log.d(TAG, "Fetched ${userData.size} user data entries for $userId")
                    trySend(userData).isSuccess
                } else {
                    Log.d(TAG, "User data snapshot was null for $userId")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    // Uppdatera/skapa status för ett land för en användare
    override suspend fun updateUserCountryStatus(userId: String, country: String, status: CountryStatus) {
        if (userId.isEmpty() || country.isEmpty()) {
            Log.w(TAG, "Cannot update status with empty userId or country")
            return
        }
        val docId = "${userId}_${country}" // Skapa det sammansatta dokument-IDt
        val docRef = firestore.collection(USER_DATA_COLLECTION).document(docId)

        val newStatusString = status.toFirestoreString() // Konvertera enum till string/null

        try {
            // Hämta befintligt dokument för att se om det finns
            val currentDoc = docRef.get().await()
            if (currentDoc.exists()) {
                // Om dokumentet finns, uppdatera bara status och updatedAt
                if (newStatusString == null) {
                    // Om status är NOT_VISITED, kan vi ta bort betyget också om det är logiskt
                    // eller bara sätta status till null. Här uppdaterar vi bara status.
                    Log.d(TAG, "Updating status to null for $docId")
                    docRef.update(mapOf(
                        "status" to null, // Sätt till null
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp() // Uppdatera timestamp
                    )).await()
                } else {
                    Log.d(TAG, "Updating status to $newStatusString for $docId")
                    docRef.update(mapOf(
                        "status" to newStatusString,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )).await()
                }

            } else if (newStatusString != null) {
                // Om dokumentet INTE finns OCH status INTE är NOT_VISITED, skapa det
                Log.d(TAG, "Creating new document $docId with status $newStatusString")
                val newData = UserCountryData(
                    userId = userId,
                    country = country,
                    status = newStatusString,
                    rating = null, // Sätt initialt betyg till null
                    updatedAt = null // Firestore sätter detta pga @ServerTimestamp
                )
                docRef.set(newData).await()
            } else {
                // Om dokumentet inte finns och status ÄR NOT_VISITED, gör ingenting.
                Log.d(TAG, "Document $docId does not exist and status is NOT_VISITED, doing nothing.")
            }
            Log.d(TAG, "Successfully updated/created status for $docId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating/creating status for $docId", e)
            // Hantera felet (t.ex. visa ett meddelande för användaren)
        }
    }

    // Uppdatera/skapa betyg för ett land för en användare
    override suspend fun updateUserCountryRating(userId: String, country: String, rating: Int?) {
        if (userId.isEmpty() || country.isEmpty()) {
            Log.w(TAG, "Cannot update rating with empty userId or country")
            return
        }
        val docId = "${userId}_${country}"
        val docRef = firestore.collection(USER_DATA_COLLECTION).document(docId)
        val ratingLong = rating?.toLong() // Konvertera Int? till Long?

        try {
            val currentDoc = docRef.get().await()
            if (currentDoc.exists()) {
                // Dokument finns, uppdatera betyg
                Log.d(TAG, "Updating rating to $rating for $docId")
                docRef.update(mapOf(
                    "rating" to ratingLong, // Uppdatera med Long?
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )).await()
            } else if (rating != null) {
                // Dokument finns inte, men ett betyg ska sättas
                // Skapa nytt dokument med detta betyg (och okänd/null status?)
                Log.d(TAG, "Creating new document $docId with rating $rating")
                val newData = UserCountryData(
                    userId = userId,
                    country = country,
                    status = null, // Sätt ingen status initialt? Eller default?
                    rating = ratingLong,
                    updatedAt = null
                )
                docRef.set(newData).await()
            } else {
                Log.d(TAG, "Document $docId does not exist and rating is null, doing nothing.")
            }
            Log.d(TAG, "Successfully updated/created rating for $docId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating/creating rating for $docId", e)
            // Hantera felet
        }
    }
}