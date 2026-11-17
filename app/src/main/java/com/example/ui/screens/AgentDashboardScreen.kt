package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AgentSettings
import com.example.data.AvailabilityStatus
import com.example.data.CallRecord
import com.example.data.SystemReadiness
import com.example.ui.components.OutcomeBadge
import com.example.ui.components.PulsingAgentOrb
import com.example.ui.components.StatusPill
import com.example.ui.components.UrgencyChip
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.MonoLabelStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AgentDashboardScreen(
    settings: AgentSettings,
    readiness: SystemReadiness,
    recentCalls: List<CallRecord>,
    onToggleAgent: (Boolean) -> Unit,
    onSelectAvailability: (AvailabilityStatus) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onSimulateCall: (callerName: String, callerNumber: String) -> Unit,
    onOpenInbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomSimDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroAgentShieldCard(
                    settings = settings,
                    onToggleAgent = onToggleAgent
                )
            }

            if (!readiness.isCoreReady || (readiness.isCallScreeningSupported && !readiness.hasCallScreeningRole)) {
                item {
                    ReadinessSetupCard(
                        readiness = readiness,
                        onRequestPermissions = onRequestPermissions,
                        onRequestCallScreeningRole = onRequestCallScreeningRole
                    )
                }
            }

            item {
                AvailabilitySelectorSection(
                    current = settings.availabilityStatus,
                    availabilityNote = settings.availabilityNote,
                    onSelect = onSelectAvailability
                )
            }

            item {
                SimulateCallSection(
                    onSimulatePreset = onSimulateCall,
                    onOpenCustomDialog = { showCustomSimDialog = true }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Handled Calls",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (recentCalls.isNotEmpty()) {
                        TextButton(
                            onClick = onOpenInbox,
                            modifier = Modifier.testTag("view_all_inbox_button")
                        ) {
                            Text("View All (${recentCalls.size})")
                        }
                    }
                }
            }

            if (recentCalls.isEmpty()) {
                item {
                    EmptyRecentCallsCard(
                        onTrySimulator = {
                            onSimulateCall("Rohan Mehta", "+91 98765 43210")
                        }
                    )
                }
            } else {
                items(recentCalls.take(3), key = { it.id }) { record ->
                    RecentCallSummaryCard(record = record, onClick = onOpenInbox)
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showCustomSimDialog) {
        CustomSimulatedCallDialog(
            onDismiss = { showCustomSimDialog = false },
            onStart = { name, number ->
                showCustomSimDialog = false
                onSimulateCall(name, number)
            }
        )
    }
}

@Composable
private fun HeroAgentShieldCard(
    settings: AgentSettings,
    onToggleAgent: (Boolean) -> Unit
) {
    val active = settings.shouldAutoAnswerCalls
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_call_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.28f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = if (active) "AUTO-ANSWER ARMED" else if (settings.isAgentEnabled) "STANDBY" else "PAUSED",
                        active = active
                    )
                    Switch(
                        checked = settings.isAgentEnabled,
                        onCheckedChange = onToggleAgent,
                        modifier = Modifier.testTag("agent_master_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PulsingAgentOrb(
                        isActive = active,
                        isSpeaking = false,
                        size = 70.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${settings.agentName} · Call Assistant",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (!settings.isAgentEnabled) {
                                stringResource(R.string.status_shield_paused)
                            } else if (active) {
                                "Answers after ${settings.autoAnswerDelaySeconds}s · Greets, checks FAQs & captures messages"
                            } else {
                                "You are Available — calls ring through normally"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessSetupCard(
    readiness: SystemReadiness,
    onRequestPermissions: () -> Unit,
    onRequestCallScreeningRole: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Enable Real Incoming Call Handling",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Grant incoming call state, auto-answer, and microphone audio access so ${stringResource(R.string.app_name)} can attend real calls and talk with callers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionMiniChip(
                    label = "Call State",
                    granted = readiness.hasPhoneStatePermission,
                    modifier = Modifier.weight(1f)
                )
                PermissionMiniChip(
                    label = "Auto-Answer",
                    granted = readiness.hasAnswerCallsPermission,
                    modifier = Modifier.weight(1f)
                )
                PermissionMiniChip(
                    label = "Audio / Mic",
                    granted = readiness.hasRecordAudioPermission,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!readiness.isCoreReady) {
                    FilledTonalButton(
                        onClick = onRequestPermissions,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grant_permissions_button")
                    ) {
                        Text(
                            text = "Setup Permissions",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (readiness.isCallScreeningSupported && !readiness.hasCallScreeningRole) {
                    OutlinedButton(
                        onClick = onRequestCallScreeningRole,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grant_screening_role_button")
                    ) {
                        Text(
                            text = "Call Screener Role",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionMiniChip(
    label: String,
    granted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (granted) EmeraldLive.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = if (granted) "$label granted" else "$label needed",
                tint = if (granted) EmeraldLive else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AvailabilitySelectorSection(
    current: AvailabilityStatus,
    availabilityNote: String,
    onSelect: (AvailabilityStatus) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Your Availability",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AvailabilityStatus.entries.take(2).forEach { status ->
                AvailabilityTile(
                    status = status,
                    selected = status == current,
                    onClick = { onSelect(status) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AvailabilityStatus.entries.drop(2).forEach { status ->
                AvailabilityTile(
                    status = status,
                    selected = status == current,
                    onClick = { onSelect(status) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        AnimatedVisibility(visible = availabilityNote.isNotBlank()) {
            Text(
                text = "Active note shared with callers: \"$availabilityNote\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AvailabilityTile(
    status: AvailabilityStatus,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("availability_${status.name.lowercase()}"),
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = if (status.autoAnswerRecommended) "AI Auto-Answers" else "Rings Normally",
                style = MaterialTheme.typography.labelSmall,
                color = if (status.autoAnswerRecommended) {
                    MaterialTheme.colorScheme.primary
                } else {
                    EmeraldLive
                }
            )
        }
    }
}

@Composable
private fun SimulateCallSection(
    onSimulatePreset: (callerName: String, callerNumber: String) -> Unit,
    onOpenCustomDialog: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Test Live AI Call Handling",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Simulate an incoming call right now to hear how your AI agent greets callers, answers FAQs, takes messages, or redirects urgent calls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetCallerButton(
                    label = "Delivery",
                    icon = Icons.Rounded.LocalShipping,
                    onClick = { onSimulatePreset("BlueDart Courier", "+91 98201 11223") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_delivery_button")
                )
                PresetCallerButton(
                    label = "Client",
                    icon = Icons.Rounded.WorkOutline,
                    onClick = { onSimulatePreset("Priya Sharma", "+91 99104 55678") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_client_button")
                )
                PresetCallerButton(
                    label = "Urgent",
                    icon = Icons.Rounded.NotificationsActive,
                    onClick = { onSimulatePreset("Mom", "+91 98111 00998") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_urgent_button")
                )
            }

            Button(
                onClick = onOpenCustomDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("simulate_custom_call_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneInTalk,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_test_call),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun PresetCallerButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun RecentCallSummaryCard(
    record: CallRecord,
    onClick: () -> Unit
) {
    val timeText = remember(record.startedAtMillis) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(record.startedAtMillis))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.callerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${record.callerNumber} · $timeText",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UrgencyChip(urgency = record.urgency)
                    OutcomeBadge(outcome = record.outcome)
                }
            }
            Text(
                text = record.capturedMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyRecentCallsCard(onTrySimulator: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "No AI-handled calls yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "When someone calls while you're Busy, In a Meeting, or Away, your AI agent answers automatically and logs their message here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onTrySimulator,
                modifier = Modifier.testTag("empty_try_simulator_button")
            ) {
                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Try Sample Call")
            }
        }
    }
}

@Composable
private fun CustomSimulatedCallDialog(
    onDismiss: () -> Unit,
    onStart: (String, String) -> Unit
) {
    var callerName by remember { mutableStateOf("Rohan Verma") }
    var callerNumber by remember { mutableStateOf("+91 98765 43210") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulate Incoming Call") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Test your live Gemini AI voice agent with a custom caller profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = callerName,
                    onValueChange = { callerName = it },
                    label = { Text("Caller Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = callerNumber,
                    onValueChange = { callerNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(callerName, callerNumber) },
                modifier = Modifier.testTag("confirm_simulate_call_button")
            ) {
                Text("Ring Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
