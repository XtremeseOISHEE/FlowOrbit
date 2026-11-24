package com.example.floworbit.presentation.profile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.floworbit.data.auth.AuthRepository
import com.example.floworbit.data.profile.ProfilePictureRepository
import com.example.floworbit.data.profile.UserProfileRepository
import com.example.floworbit.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val userProfile: UserProfile? = null,
    val userEmail: String? = null,
    val profilePicture: Bitmap? = null, // To hold the decoded image
    val error: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository = UserProfileRepository()
    private val authRepository = AuthRepository()
    private val pictureRepository = ProfilePictureRepository(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = profileRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in.") }
                return@launch
            }

            // Fetch both Firestore profile and Room profile picture
            val profile = profileRepository.getUserProfile(userId)
            val currentUser = authRepository.getCurrentUser()
            val profilePicture = pictureRepository.getProfilePicture(userId)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    userProfile = profile,
                    userEmail = currentUser?.email,
                    profilePicture = profilePicture
                )
            }
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            val userId = profileRepository.getCurrentUserId() ?: return@launch
            profileRepository.updateUsername(userId, newUsername)
            loadUserProfile()
        }
    }

    fun saveProfilePicture(imageData: ByteArray) {
        viewModelScope.launch {
            val userId = profileRepository.getCurrentUserId() ?: return@launch
            val bitmap = imageData.toBitmap() ?: return@launch
            pictureRepository.saveProfilePicture(userId, bitmap)
            // Refresh the UI with the new picture
            _uiState.update { it.copy(profilePicture = bitmap) }
        }
    }

    fun logout() {
        authRepository.logout()
    }

    // Helper function to convert ByteArray to Bitmap
    private fun ByteArray.toBitmap(): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(this, 0, this.size)
        } catch (e: Exception) {
            null
        }
    }
}
