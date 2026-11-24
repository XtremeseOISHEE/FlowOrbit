package com.example.floworbit.presentation.focus

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.floworbit.presentation.dnd.DNDViewModel
import com.example.floworbit.util.formatMillis

private val PRESET_MINUTES = listOf(15, 25, 45, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    onBack: () -> Unit,
    onEditBlockedApps: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as Application
    val dndViewModel: DNDViewModel = viewModel()

    val focusViewModel: FocusTimerViewModel = viewModel(
        factory = FocusTimerViewModelFactory(app, dndViewModel)
    )

    val remainingMillis by focusViewModel.remaining.collectAsState()
    val isRunning by focusViewModel.running.collectAsState()
    val boringModeActive by focusViewModel.boringModeActive.collectAsState()
    val boringModeAccessMissing by focusViewModel.boringModeAccessMissing.collectAsState()
    val blockedCount by focusViewModel.blockedCount.collectAsState()

    LaunchedEffect(boringModeAccessMissing) {
        if (boringModeAccessMissing) {
            android.widget.Toast.makeText(
                app,
                "Boring Mode needs WRITE_SECURE_SETTINGS granted via adb first.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            focusViewModel.consumeBoringModeAccessWarning()
        }
    }

    val timerState = when {
        isRunning -> TimerState.RUNNING
        remainingMillis > 0 && !isRunning -> TimerState.PAUSED
        else -> TimerState.STOPPED
    }

    var customMinutes by remember { mutableStateOf("25") }

    val totalTimeMillis = (customMinutes.toLongOrNull() ?: 25) * 60 * 1000
    val started = timerState != TimerState.STOPPED
    val elapsed = if (totalTimeMillis > 0 && started) {
        (1f - remainingMillis.toFloat() / totalTimeMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val sweep = if (started) maxOf(elapsed, 0.006f) else 0f

    val phaseLabel = when {
        isRunning -> "In orbit"
        started && remainingMillis <= 0L -> "Session complete"
        started -> "Paused"
        else -> "Ready"
    }
    val progressNote = if (started) {
        "${(elapsed * 100).toInt()}% elapsed · ${customMinutes.toIntOrNull() ?: 25} min goal"
    } else {
        "${customMinutes.toIntOrNull() ?: 25} min goal"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Boring Mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = boringModeActive,
                    onCheckedChange = { focusViewModel.toggleBoringMode() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            FocusRing(
                sweepFraction = sweep,
                started = started,
                phaseLabel = phaseLabel,
                clockText = formatMillis(remainingMillis.takeIf { started } ?: totalTimeMillis),
                progressNote = progressNote
            )

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = "$blockedCount apps will be blocked",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (timerState == TimerState.STOPPED) {
                Spacer(modifier = Modifier.height(30.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "FOCUS DURATION (MINUTES)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(11.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .padding(start = 18.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = customMinutes,
                            onValueChange = { customMinutes = it.filter { c -> c.isDigit() }.take(3) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                        )
                        StepperButton(icon = Icons.Filled.Remove, contentDescription = "Decrease") {
                            val current = customMinutes.toIntOrNull() ?: 25
                            customMinutes = maxOf(5, current - 5).toString()
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        StepperButton(icon = Icons.Filled.Add, contentDescription = "Increase") {
                            val current = customMinutes.toIntOrNull() ?: 25
                            customMinutes = minOf(180, current + 5).toString()
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_MINUTES.forEach { minutes ->
                            val active = customMinutes.toIntOrNull() == minutes
                            Text(
                                text = "$minutes min",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.surfaceContainer
                                        else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (active) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(11.dp)
                                    )
                                    .clickable { customMinutes = minutes.toString() }
                                    .padding(vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(34.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (timerState != TimerState.STOPPED) {
                    OutlinedButton(
                        onClick = { focusViewModel.stop() },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape
                    ) {
                        Text("Stop", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Button(
                    onClick = {
                        when (timerState) {
                            TimerState.STOPPED -> {
                                val minutes = customMinutes.toIntOrNull() ?: 25
                                focusViewModel.start(minutes)
                            }
                            TimerState.PAUSED -> focusViewModel.start(0)
                            TimerState.RUNNING -> focusViewModel.pause()
                        }
                    },
                    modifier = Modifier.size(110.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(0.dp),
                    enabled = !(timerState == TimerState.STOPPED && (customMinutes.toIntOrNull() ?: 0) <= 0)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (timerState == TimerState.RUNNING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val buttonText = when (timerState) {
                            TimerState.RUNNING -> "Pause"
                            TimerState.PAUSED -> "Resume"
                            TimerState.STOPPED -> "Start"
                        }
                        Text(buttonText, style = MaterialTheme.typography.labelMedium, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (started) "Reset session" else "Blocked apps · edit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                modifier = Modifier
                    .clickable {
                        if (started) focusViewModel.stop() else onEditBlockedApps()
                    }
                    .padding(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FocusRing(
    sweepFraction: Float,
    started: Boolean,
    phaseLabel: String,
    clockText: String,
    progressNote: String
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val leadingEdgeColor = MaterialTheme.colorScheme.secondary
    val midLayer = MaterialTheme.colorScheme.surfaceContainerHighest
    val midLayerBorder = MaterialTheme.colorScheme.outlineVariant
    val innerLayer = MaterialTheme.colorScheme.background
    val innerLayerBorder = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val noteColor = MaterialTheme.colorScheme.tertiary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        // Concentric depth layers, echoing the mockup's "three surfaces" ring.
        Box(
            modifier = Modifier
                .size(236.dp)
                .clip(CircleShape)
                .background(midLayer)
                .border(1.dp, midLayerBorder, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(188.dp)
                .clip(CircleShape)
                .background(innerLayer)
                .border(1.dp, innerLayerBorder, CircleShape)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
            )
            if (started) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360 * sweepFraction,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = leadingEdgeColor,
                    startAngle = -90f,
                    sweepAngle = 360 * sweepFraction,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = phaseLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = clockText,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Light),
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = progressNote,
                style = MaterialTheme.typography.labelMedium,
                color = noteColor
            )
        }
    }
}

// Ensure this factory class exists in its own file as you've mentioned
// This is just for reference
// class FocusTimerViewModelFactory(...) { ... }
