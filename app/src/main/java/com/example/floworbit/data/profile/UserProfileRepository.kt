package com.example.floworbit.data.profile

import com.example.floworbit.domain.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.tasks.await

class UserProfileRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun createProfileForNewUser(userId: String, email: String) {
        val username = email.substringBefore('@').replace(".", "").take(15)
        val newUserProfile = UserProfile(
            userId = userId,
            username = username,
            title = "Noob",
            coins = 0,
            totalSessionsCompleted = 0
        )
        usersCollection.document(userId).set(newUserProfile).await()
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val document = usersCollection.document(userId).get().await()
            document.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUsername(userId: String, newUsername: String) {
        if (newUsername.isNotBlank()) {
            usersCollection.document(userId).update("username", newUsername).await()
        }
    }

    suspend fun incrementSessionCount(userId: String, amount: Long = 1, coinsToAdd: Long = 1) {
        val userDocRef = usersCollection.document(userId)
        db.runTransaction(Transaction.Function { transaction ->
            val snapshot = transaction.get(userDocRef)

            if (snapshot.exists()) {
                // --- If the profile exists, update it ---
                val currentSessions = snapshot.getLong("totalSessionsCompleted") ?: 0
                val currentCoins = snapshot.getLong("coins") ?: 0

                val newTotalSessions = currentSessions + amount
                val newCoins = currentCoins + coinsToAdd

                val newTitle = when (newTotalSessions) {
                    in 0..4 -> "Noob"
                    in 5..19 -> "Streak Starter"
                    in 20..49 -> "Streakmaster"
                    else -> "Prodigy"
                }

                transaction.update(userDocRef, "totalSessionsCompleted", newTotalSessions)
                transaction.update(userDocRef, "coins", newCoins)
                transaction.update(userDocRef, "title", newTitle)
            } else {
                // --- If the profile does NOT exist, create it ---
                val email = auth.currentUser?.email ?: ""
                val username = email.substringBefore('@').replace(".", "").take(15)
                val newProfile = UserProfile(
                    userId = userId,
                    username = username,
                    totalSessionsCompleted = 1, // First session
                    coins = 1, // First coin
                    title = "Noob"
                )
                transaction.set(userDocRef, newProfile)
            }
            null
        }).await()
    }
}
