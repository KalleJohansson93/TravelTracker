package com.example.traveltracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

// Ny Data Source för användarens profil (username)
class FirestoreUserDataDataSource(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    fun getUserProfile(): Flow<UserProfile?> {
        val userId = auth.currentUser?.uid ?: return flowOf(null) // Returnera null om ingen inloggad
        return firestore.collection("users").document(userId)
            .snapshots() // Observera realtidsändringar i profilen
            .map { documentSnapshot ->
                // Mappa dokumentet till UserProfile data class
                documentSnapshot.toObject<UserProfile>()
            }
    }
}

// Ny Data Source för aggregerad global statistik
class FirestoreGlobalStatsDataSource(
    private val firestore: FirebaseFirestore
) {
    fun getGlobalStats(): Flow<GlobalStats?> {
        // Observera det enda dokumentet för global statistik
        return firestore.collection("statistics").document("globalStats")
            .snapshots()
            .map { documentSnapshot ->
                // Mappa dokumentet till GlobalStats data class
                documentSnapshot.toObject<GlobalStats>()
            }
    }
}