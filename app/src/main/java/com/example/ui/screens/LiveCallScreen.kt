package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhoneForwarded
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.CallPhase
import com.example.data.LiveCallState
import com.example.data.SpeakerRole
import com.example.data.TranscriptMessage
import com.example.ui.components.AcousticWaveformVisualizer
import com.example.ui.components.PulsingAgentOrb
import com.example.ui.components.StatusPill
import com.example.ui.components.UrgencyChip
import com.example.ui.components.formatDuration
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CoralHangup
import com.example.ui.theme.MonoLabelStyle

@Composable
fun LiveCallScreen(
    liveState: LiveCallState,
    agentName: String,
    hasAudioPermission: Boolean,
    onSendCallerInput: (String) -> Unit,
    onTriggerMic: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRedirectNow: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    BackHandler(enabled = liveState.isSimulated) {
        onEndCall()
    }

    LaunchedEffect(liveState.transcript.size) {
        if (liveState.transcript.isNotEmpty()) {
            listState.animateScrollToItem(liveState.transcript.lastIndex)
        }
    }

    val quickCallerPrompts = remember {
        listOf(
            "Hello, is he available right now?",
            "Where should I leave the delivery?",
            "Please take a message: call me back after 5 PM.",
            "It's urgent, please transfer my call!",
            "Bhaiya kab free honge? Urgent baat karni hai."
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with caller info + timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    label = if (liveState.isSimulated) "SIMULATED CALL" else "LIVE CELLULAR CALL",
                    active = true
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UrgencyChip(urgency = liveState.urgency)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = formatDuration(liveState.elapsedSeconds),
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Center acoustic card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PulsingAgentOrb(
                            isActive = liveState.phase != CallPhase.ENDED,
                            isSpeaking = liveState.isSpeakingAudio || liveState.phase == CallPhase.THINKING,
                            size = 56.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = liveState.capturedCallerName ?: liveState.callerName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = liveState.callerNumber,
                                style = MonoLabelStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = liveState.statusBanner,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = liveState.currentIntent,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    AcousticWaveformVisualizer(
                        isAnimating = liveState.isSpeakingAudio ||
                            liveState.isListeningAudio ||
                            liveState.phase == CallPhase.THINKING ||
                            liveState.phase == CallPhase.RINGING_COUNTDOWN,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    )
                }
            }

            // Captured message / Redirect notice
            AnimatedVisibility(visible = !liveState.capturedMessage.isNullOrBlank() || liveState.redirectRequested) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (liveState.redirectRequested) {
                            AmberAlert.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (liveState.redirectRequested) AmberAlert else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!liveState.capturedMessage.isNullOrBlank()) {
                            Text(
                                text = "Captured Message: ${liveState.capturedMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (liveState.redirectRequested) {
                            Text(
                                text = "Redirect Triggered: ${liveState.redirectReason ?: "Urgent / Caller requested transfer"} -> ${liveState.redirectTargetName ?: "Forward Target"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberAlert
                            )
                        }
                    }
                }
            }

            // Transparent AI / Permission Error banner
            AnimatedVisibility(visible = liveState.aiError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = liveState.aiError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        if (!hasAudioPermission) {
                            androidx.compose.material3.TextButton(
                                onClick = onRequestAudioPermission,
                                modifier = Modifier.testTag("live_call_allow_mic_button")
                            ) {
                                Text("Allow Mic")
                            }
                        }
                    }
                }
            }

            // Live conversation transcript
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(liveState.transcript, key = { it.id }) { msg ->
                    TranscriptBubble(message = msg, agentName = agentName)
                }
            }

            // Quick caller prompts for effortless simulator testing
            if (liveState.phase != CallPhase.RINGING_COUNTDOWN) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickCallerPrompts.forEachIndexed { index, prompt ->
                        AssistChip(
                            onClick = { onSendCallerInput(prompt) },
                            label = { Text(prompt, maxLines = 1) },
                            modifier = Modifier.testTag("quick_caller_prompt_$index")
                        )
                    }
                }
            }

            // Caller voice / text input bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = {
                        if (hasAudioPermission) {
                            onTriggerMic()
                        } else {
                            onRequestAudioPermission()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (liveState.isListeningAudio) {
                            CoralHangup
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("live_call_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = if (hasAudioPermission) {
                            "Speak as caller"
                        } else {
                            "Grant microphone permission to speak"
                        }
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Speak or type caller reply...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSendCallerInput(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_call_text_input")
                )

                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendCallerInput(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("live_call_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send caller reply"
                    )
                }
            }

            // Action row: Redirect & End Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRedirectNow,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("live_call_redirect_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneForwarded,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Redirect")
                }

                Button(
                    onClick = onEndCall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralHangup,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("live_call_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("End Call")
                }
            }
        }
    }
}

@Composable
private fun TranscriptBubble(
    message: TranscriptMessage,
    agentName: String
) {
    val isAgent = message.speaker == SpeakerRole.AGENT
    val isSystem = message.speaker == SpeakerRole.SYSTEM

    if (isSystem) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAgent) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            color = if (isAgent) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isAgent) 4.dp else 18.dp,
                bottomEnd = if (isAgent) 18.dp else 4.dp
            ),
            border = BorderStroke(
                1.dp,
                if (isAgent) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                }
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isAgent) "$agentName (AI)" else "Caller",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAgent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isAgent) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
        }
    }
}
