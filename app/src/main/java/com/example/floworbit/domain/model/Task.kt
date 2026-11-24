package com.example.floworbit.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Task(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val priority: Int = 0,
    val category: String? = null,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val reminderAt: Timestamp? = null,
    val reminderMessage: String? = null
)
