package com.example.floworbit.data.auth

import com.example.floworbit.data.profile.UserProfileRepository
import com.example.floworbit.presentation.auth.AuthResult
import com.example.floworbit.presentation.auth.UserData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userProfileRepository = UserProfileRepository() // Create an instance

    fun getCurrentUser(): UserData? {
        val firebaseUser = firebaseAuth.currentUser
        return firebaseUser?.let {
            UserData(
                userId = it.uid,
                username = it.displayName,
                email = it.email
            )
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthResult(data = UserData(result.user!!.uid, result.user!!.displayName, result.user!!.email))
        } catch (e: Exception) {
            AuthResult(errorMessage = e.message)
        }
    }

    suspend fun register(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val newUser = result.user
            if (newUser != null) {
                // ⭐ After successful registration, create the profile in Firestore
                userProfileRepository.createProfileForNewUser(newUser.uid, newUser.email ?: "")
                AuthResult(data = UserData(newUser.uid, newUser.displayName, newUser.email))
            } else {
                AuthResult(errorMessage = "Failed to create user.")
            }
        } catch (e: Exception) {
            AuthResult(errorMessage = e.message)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult(data = UserData(userId = "", username = null, email = null)) // Success
        } catch (e: Exception) {
            AuthResult(errorMessage = e.message)
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}
