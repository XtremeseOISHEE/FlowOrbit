package com.example.floworbit.presentation.task

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.floworbit.domain.model.Task
import com.example.floworbit.presentation.home.AppViewModelFactory
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    onAddTask: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val taskViewModel: TaskViewModel = viewModel(factory = AppViewModelFactory(app))
    val tasks by taskViewModel.tasks.collectAsState()

    // Sort tasks by priority (descending) and then by creation date (descending, nulls last)
    val sortedTasks = tasks.sortedWith(
        compareByDescending { task: Task -> task.priority }
            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tasks") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                shape = RoundedCornerShape(22.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(sortedTasks) { task ->
                TaskItem(
                    task = task,
                    onClick = { onTaskClick(task.id) },
                    onToggleComplete = { taskViewModel.updateTask(task.copy(completed = !task.completed)) }
                )
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onClick: () -> Unit, onToggleComplete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            // Open tasks sit on the raised card surface; completed ones drop to the flat
            // background so the list visibly thins out as the day's work gets done.
            containerColor = if (task.completed) MaterialTheme.colorScheme.background
                             else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = if (task.completed) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.completed) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (task.completed) MaterialTheme.colorScheme.secondary
                        else androidx.compose.ui.graphics.Color.Transparent
                    )
                    .then(
                        if (!task.completed) Modifier.border(
                            1.5.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(9.dp)
                        ) else Modifier
                    )
                    .clickable(onClick = onToggleComplete),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.completed) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                )
                task.createdAt?.toDate()?.let {
                    Text(
                        text = java.text.SimpleDateFormat("MMM dd, yyyy").format(it),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
