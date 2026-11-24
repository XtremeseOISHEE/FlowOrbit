package com.example.floworbit.presentation.home

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.floworbit.data.profile.ProfilePictureRepository
import com.example.floworbit.data.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val pictureRepository: ProfilePictureRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _profilePicture = MutableStateFlow<Bitmap?>(null)
    val profilePicture = _profilePicture.asStateFlow()

    init {
        loadProfilePicture()
    }

    fun loadProfilePicture() {
        viewModelScope.launch {
            val userId = userProfileRepository.getCurrentUserId()
            if (userId != null) {
                _profilePicture.value = pictureRepository.getProfilePicture(userId)
            }
        }
    }
}
