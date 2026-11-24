package com.example.floworbit.presentation.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.floworbit.data.api.ChatRepository
import com.example.floworbit.data.profile.ProfilePictureRepository
import com.example.floworbit.data.profile.UserProfileRepository
import com.example.floworbit.data.task.TaskRepository
import com.example.floworbit.presentation.chat.ChatViewModel
import com.example.floworbit.presentation.task.TaskViewModel

class AppViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                pictureRepository = ProfilePictureRepository(app),
                userProfileRepository = UserProfileRepository()
            ) as T
        }
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(
                taskRepository = TaskRepository()
            ) as T
        }
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(
                repo = ChatRepository(context = app)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
