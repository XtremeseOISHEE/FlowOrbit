package com.example.floworbit.presentation.chat

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.floworbit.presentation.home.AppViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: ChatViewModel = viewModel(factory = AppViewModelFactory(app))
    val uiState by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    // This modifier respects the system navigation bar, pushing the input field up
                    .navigationBarsPadding()
                    // Apply our own content padding after handling the system inset
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Ask me anything…") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isProcessing
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        viewModel.sendMessage(input)
                        input = ""
                    },
                    enabled = input.isNotBlank() && !uiState.isProcessing
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp), // Adjusted padding for avatar
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom) // Consistent spacing
        ) {
            // The "thinking" indicator is ephemeral UI state, not a persisted message, so it's
            // rendered here rather than living in the Room-backed message list.
            if (uiState.isProcessing) {
                item { ThinkingBubble() }
            }
            items(uiState.messages.asReversed()) { message ->
                MessageBubble(message)
            }
        }
    }
}

@Composable
fun AiAvatar() {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "F",
            color = MaterialTheme.colorScheme.onTertiary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun ThinkingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        AiAvatar()
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.sender == Sender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom // Align avatar with bubble bottom
    ) {
        // AI Avatar - only show for AI messages
        if (!isUser) {
            AiAvatar()
        }

        // Rounded, shadowed bubble with a max width. Speaker is carried by side + avatar + corner
        // notch, not just color, so it still reads once Boring Mode desaturates everything —
        // user bubbles get a teal-tinted fill, the assistant's sits on the plain card surface.
        Card(
            shape = if (isUser) RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
                    else RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // Subtle shadow
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp) // Constrain width
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isUser) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
