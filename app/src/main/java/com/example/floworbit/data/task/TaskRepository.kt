package com.example.floworbit.data.task

import com.example.floworbit.domain.model.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TaskRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    fun getTasks(): Flow<List<Task>> = callbackFlow {
        val userId = getUserId()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
        val listenerRegistration = tasksCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val tasks = snapshot.documents.mapNotNull { doc ->
                        // Manual deserialization to prevent crashes from bad data
                        try {
                            val task = doc.toObject<Task>()
                            task?.copy(id = doc.id)
                        } catch (e: Exception) {
                            // Log the error or handle it, for now, we just skip the bad document
                            null
                        }
                    }
                    trySend(tasks).isSuccess
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addTask(task: Task) {
        val userId = getUserId() ?: return
        val taskWithUserId = task.copy(userId = userId, createdAt = Timestamp.now())
        firestore.collection("users").document(userId).collection("tasks")
            .add(taskWithUserId).await()
    }

    suspend fun updateTask(task: Task) {
        val userId = getUserId() ?: return
        if (task.id.isBlank()) return
        val taskWithUserId = task.copy(userId = userId)
        firestore.collection("users").document(userId).collection("tasks")
            .document(task.id)
            .set(taskWithUserId).await()
    }

    suspend fun deleteTask(id: String) {
        val userId = getUserId() ?: return
        if (id.isBlank()) return
        firestore.collection("users").document(userId).collection("tasks")
            .document(id)
            .delete().await()
    }

    suspend fun getTaskById(id: String): Task? {
        val userId = getUserId() ?: return null
        if (id.isBlank()) return null
        val document = firestore.collection("users").document(userId).collection("tasks")
            .document(id)
            .get().await()
        return document.toObject(Task::class.java)?.copy(id = document.id)
    }
}
