package com.example.floworbit.presentation.task

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.floworbit.domain.model.Priority
import com.example.floworbit.domain.model.Task
import com.example.floworbit.presentation.home.AppViewModelFactory
import com.example.floworbit.service.NotificationReceiver
import com.google.firebase.Timestamp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(taskId: String, onBack: () -> Unit) {

    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: TaskViewModel = viewModel(factory = AppViewModelFactory(app))

    var task by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(taskId) {
        if (taskId.isNotBlank()) {
            vm.viewModelScope.launch {
                task = vm.getTaskById(taskId)
            }
        }
    }

    // State for all the editable fields
    var title by remember(task) { mutableStateOf(task?.title ?: "") }
    var description by remember(task) { mutableStateOf(task?.description ?: "") }
    var category by remember(task) { mutableStateOf(task?.category ?: "") }
    var priority by remember(task) { mutableStateOf(Priority.fromInt(task?.priority ?: 0)) }

    // Reminder states
    var reminderDate by remember(task) { mutableStateOf(task?.reminderAt?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()) }
    var reminderTime by remember(task) { mutableStateOf(task?.reminderAt?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime()) }
    var reminderMessage by remember(task) { mutableStateOf(task?.reminderMessage ?: "") }

    val dateDialogState = rememberMaterialDialogState()
    val timeDialogState = rememberMaterialDialogState()


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (taskId.isBlank()) "Add Task" else "Edit Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Priority", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { prio ->
                    FilterChip(
                        selected = priority == prio,
                        onClick = { priority = prio },
                        label = { Text(prio.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Reminder", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = reminderDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                    onValueChange = { },
                    label = { Text("Date") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { dateDialogState.show() },
                    readOnly = true,
                    enabled = false
                )
                OutlinedTextField(
                    value = reminderTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "",
                    onValueChange = { },
                    label = { Text("Time") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { timeDialogState.show() },
                    readOnly = true,
                    enabled = false
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = reminderMessage,
                onValueChange = { reminderMessage = it },
                label = { Text("Reminder Message") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(
                    onClick = {
                        val reminderAt = if (reminderDate != null && reminderTime != null) {
                            val zonedDateTime = reminderDate!!.atTime(reminderTime!!).atZone(ZoneId.systemDefault())
                            val instant = zonedDateTime.toInstant()
                            Timestamp(instant.epochSecond, instant.nano)
                        } else {
                            null
                        }

                        val taskToSave = if (taskId.isBlank()) {
                            val newId = UUID.randomUUID().toString()
                            Task(
                                id = newId,
                                title = title,
                                description = description,
                                category = if (category.isBlank()) null else category,
                                reminderAt = reminderAt,
                                reminderMessage = if (reminderMessage.isBlank()) null else reminderMessage,
                                priority = priority.value
                            )
                        } else {
                            task!!.copy(
                                title = title,
                                description = description,
                                category = if (category.isBlank()) null else category,
                                priority = priority.value,
                                reminderAt = reminderAt,
                                reminderMessage = if (reminderMessage.isBlank()) null else reminderMessage
                            )
                        }

                        // Schedule alarm
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val intent = Intent(context, NotificationReceiver::class.java).apply {
                            putExtra("taskId", taskToSave.id)
                            putExtra("title", taskToSave.title)
                            putExtra("message", taskToSave.reminderMessage)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            taskToSave.id.hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        if (reminderAt != null) {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                reminderAt.toDate().time,
                                pendingIntent
                            )
                        } else {
                            alarmManager.cancel(pendingIntent)
                        }

                        if (taskId.isBlank()) {
                            vm.viewModelScope.launch { vm.addTask(taskToSave) }
                        } else {
                            vm.viewModelScope.launch { vm.updateTask(taskToSave) }
                        }

                        onBack()
                    }
                ) { Text("Save") }

                if (taskId.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            task?.let {
                                // Cancel any existing alarm when deleting
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                val intent = Intent(context, NotificationReceiver::class.java)
                                val pendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    it.id.hashCode(),
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                alarmManager.cancel(pendingIntent)

                                vm.deleteTask(it.id)
                                onBack()
                            }
                        }
                    ) { Text("Delete") }
                }
            }
        }
    }

    MaterialDialog(
        dialogState = dateDialogState,
        buttons = {
            positiveButton("Ok")
            negativeButton("Cancel")
        }
    ) {
        datepicker { date ->
            reminderDate = date
        }
    }

    MaterialDialog(
        dialogState = timeDialogState,
        buttons = {
            positiveButton("Ok")
            negativeButton("Cancel")
        }
    ) {
        timepicker { time ->
            reminderTime = time
        }
    }
}
