package com.example.traveltracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FirestoreUserDataDataSource(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    fun getUserProfile(): Flow<UserProfile?> {
        val userId = auth.currentUser?.uid ?: return flowOf(null)
        return firestore.collection("users").document(userId)
            .snapshots()
            .map { documentSnapshot ->
                documentSnapshot.toObject<UserProfile>()
            }
    }
}

class FirestoreGlobalStatsDataSource(
    private val firestore: FirebaseFirestore
) {
    fun getGlobalStats(): Flow<GlobalStats?> {
        return firestore.collection("statistics").document("globalStats")
            .snapshots()
            .map { documentSnapshot ->
                documentSnapshot.toObject<GlobalStats>()
            }
    }
}