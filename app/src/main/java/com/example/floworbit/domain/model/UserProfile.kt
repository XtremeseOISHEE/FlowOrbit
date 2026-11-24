package com.example.floworbit.domain.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Represents the data structure for a user's profile stored in Firestore.
 * @param userId The unique ID from Firebase Authentication.
 * @param username The user's display name, which can be edited.
 * @param totalSessionsCompleted The total number of focus sessions the user has finished.
 * @param coins The number of coins the user has earned.
 * @param title The user's current title (e.g., "Noob", "Prodigy").
 */
@IgnoreExtraProperties // This is a good practice for Firestore data classes
data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val totalSessionsCompleted: Long = 0,
    val coins: Long = 0,
    val title: String = "Noob"
)
