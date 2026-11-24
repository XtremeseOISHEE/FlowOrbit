package com.example.floworbit.presentation.blockedapps

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.floworbit.domain.model.InstalledApp
import com.example.floworbit.util.AppUtils
import com.example.floworbit.util.OverlayPermissionHelper
import com.example.floworbit.util.UsagePermissionHelper

@Composable
fun BlockedAppsScreen(vm: BlockedAppsViewModel = viewModel()) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val blockedCount = apps.count { it.isBlocked }
    val visibleApps = if (searchQuery.isBlank()) {
        apps
    } else {
        apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("App-Block Settings", style = MaterialTheme.typography.titleLarge)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "$blockedCount blocked",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Enforced only while a focus session is running",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(Modifier.height(16.dp))

            // Segmented control: "Installed Apps" reflects what's shown below (always active on
            // this screen); the other two are shortcuts into system settings, styled as ghost/
            // inactive segments so the active one reads clearly.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SegmentButton(
                    text = "Installed Apps",
                    selected = true,
                    modifier = Modifier.weight(1f),
                    onClick = { AppUtils.openInstalledAppsSettings(context) }
                )
                SegmentButton(
                    text = "Usage Access",
                    selected = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val intent = UsagePermissionHelper.requestUsageAccessIntent()
                        if (context is Activity) context.startActivity(intent)
                        else context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                )
                SegmentButton(
                    text = "Overlay",
                    selected = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val intent = OverlayPermissionHelper.requestOverlayIntent(context.packageName)
                        if (context is Activity) context.startActivity(intent)
                        else context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                )
            }

            Spacer(Modifier.height(18.dp))

            Text("Select apps to block:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(visibleApps) { app ->
                    AppItemRow(app = app, onToggle = { vm.toggleBlock(app) })
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun AppItemRow(app: InstalledApp, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (app.isBlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            app.icon?.let {
                Image(
                    bitmap = it.toBitmap().asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(app.appName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (app.isBlocked) "Blocked" else app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (app.isBlocked) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = app.isBlocked,
            onCheckedChange = { onToggle() },
            enabled = !app.packageName.equals(LocalContext.current.packageName), // own app always locked
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                // "muted navy-gray" OFF track, per the redesign spec
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}
