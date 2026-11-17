package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhoneForwarded
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AgentSettings
import com.example.data.LanguageMode
import com.example.data.RedirectMode
import com.example.data.SystemReadiness
import com.example.ui.theme.EmeraldLive
import kotlin.math.roundToInt

@Composable
fun RoutingScreen(
    settings: AgentSettings,
    readiness: SystemReadiness,
    onSaveRouting: (
        autoAnswerWhenAvailable: Boolean,
        autoAnswerDelaySeconds: Int,
        languageMode: LanguageMode,
        redirectEnabled: Boolean,
        redirectTargetName: String,
        redirectPhoneNumber: String,
        redirectMode: RedirectMode,
        redirectOnUrgent: Boolean,
        redirectOnRequest: Boolean,
        vipNumbersRaw: String
    ) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestSinglePermission: (String) -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    modifier: Modifier = Modifier
) {
    var autoAnswerWhenAvailable by remember { mutableStateOf(settings.autoAnswerWhenAvailable) }
    var delaySeconds by remember { mutableIntStateOf(settings.autoAnswerDelaySeconds) }
    var languageMode by remember { mutableStateOf(settings.languageMode) }
    var redirectEnabled by remember { mutableStateOf(settings.redirectEnabled) }
    var redirectTargetName by remember { mutableStateOf(settings.redirectTargetName) }
    var redirectPhoneNumber by remember { mutableStateOf(settings.redirectPhoneNumber) }
    var redirectMode by remember { mutableStateOf(settings.redirectMode) }
    var redirectOnUrgent by remember { mutableStateOf(settings.redirectOnUrgent) }
    var redirectOnRequest by remember { mutableStateOf(settings.redirectOnRequest) }
    var vipNumbersRaw by remember { mutableStateOf(settings.vipNumbersRaw) }
    var savedToast by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        autoAnswerWhenAvailable = settings.autoAnswerWhenAvailable
        delaySeconds = settings.autoAnswerDelaySeconds
        languageMode = settings.languageMode
        redirectEnabled = settings.redirectEnabled
        redirectTargetName = settings.redirectTargetName
        redirectPhoneNumber = settings.redirectPhoneNumber
        redirectMode = settings.redirectMode
        redirectOnUrgent = settings.redirectOnUrgent
        redirectOnRequest = settings.redirectOnRequest
        vipNumbersRaw = settings.vipNumbersRaw
    }

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
                Text(
                    text = "Call Routing & Redirects",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Auto-answer timing card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Auto-Answer Timing",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = "Answer after $delaySeconds seconds of ringing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = delaySeconds.toFloat(),
                            onValueChange = {
                                delaySeconds = it.roundToInt()
                                savedToast = false
                            },
                            valueRange = 0f..10f,
                            steps = 9
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-answer even when Available",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Use AI to screen every incoming call",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoAnswerWhenAvailable,
                                onCheckedChange = {
                                    autoAnswerWhenAvailable = it
                                    savedToast = false
                                }
                            )
                        }
                    }
                }
            }

            // Language selection
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Voice & Language",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        LanguageMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        languageMode = mode
                                        savedToast = false
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = languageMode == mode,
                                    onClick = {
                                        languageMode = mode
                                        savedToast = false
                                    }
                                )
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Call Redirect & Escalation Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneForwarded,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "Call Redirect Rules",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Switch(
                                checked = redirectEnabled,
                                onCheckedChange = {
                                    redirectEnabled = it
                                    savedToast = false
                                }
                            )
                        }

                        OutlinedTextField(
                            value = redirectTargetName,
                            onValueChange = {
                                redirectTargetName = it
                                savedToast = false
                            },
                            label = { Text("Redirect Contact / Desk Name") },
                            singleLine = true,
                            enabled = redirectEnabled,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = redirectPhoneNumber,
                            onValueChange = {
                                redirectPhoneNumber = it
                                savedToast = false
                            },
                            label = { Text("Redirect Phone Number") },
                            placeholder = { Text("+91 98XXXXXXXX") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            enabled = redirectEnabled,
                            modifier = Modifier.fillMaxWidth()
                        )

                        RedirectMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = redirectEnabled) {
                                        redirectMode = mode
                                        savedToast = false
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                RadioButton(
                                    selected = redirectMode == mode,
                                    enabled = redirectEnabled,
                                    onClick = {
                                        redirectMode = mode
                                        savedToast = false
                                    }
                                )
                                Column {
                                    Text(mode.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        mode.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Redirect on Urgent Calls", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = redirectOnUrgent,
                                enabled = redirectEnabled,
                                onCheckedChange = {
                                    redirectOnUrgent = it
                                    savedToast = false
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Redirect When Caller Requests", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = redirectOnRequest,
                                enabled = redirectEnabled,
                                onCheckedChange = {
                                    redirectOnRequest = it
                                    savedToast = false
                                }
                            )
                        }

                        OutlinedTextField(
                            value = vipNumbersRaw,
                            onValueChange = {
                                vipNumbersRaw = it
                                savedToast = false
                            },
                            label = { Text("VIP Bypass Numbers (comma-separated)") },
                            placeholder = { Text("Numbers that ring you directly without AI") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (savedToast) "Routing rules saved" else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = {
                                    onSaveRouting(
                                        autoAnswerWhenAvailable,
                                        delaySeconds,
                                        languageMode,
                                        redirectEnabled,
                                        redirectTargetName,
                                        redirectPhoneNumber,
                                        redirectMode,
                                        redirectOnUrgent,
                                        redirectOnRequest,
                                        vipNumbersRaw
                                    )
                                    savedToast = true
                                },
                                modifier = Modifier.testTag("save_routing_button")
                            ) {
                                Text("Save Routing")
                            }
                        }
                    }
                }
            }

            // Permissions & Call Screening Role Status
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                                text = "System Permissions & Role",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        PermissionStatusRow(
                            label = "Phone State (Incoming Ring)",
                            granted = readiness.hasPhoneStatePermission,
                            onClick = {
                                onRequestSinglePermission(android.Manifest.permission.READ_PHONE_STATE)
                            }
                        )
                        PermissionStatusRow(
                            label = "Auto-Answer Calls",
                            granted = readiness.hasAnswerCallsPermission,
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    onRequestSinglePermission(android.Manifest.permission.ANSWER_PHONE_CALLS)
                                }
                            }
                        )
                        PermissionStatusRow(
                            label = "Microphone (Caller Voice)",
                            granted = readiness.hasRecordAudioPermission,
                            onClick = {
                                onRequestSinglePermission(android.Manifest.permission.RECORD_AUDIO)
                            }
                        )
                        PermissionStatusRow(
                            label = "Auto-Transfer Calls",
                            granted = readiness.hasCallPhonePermission,
                            onClick = {
                                onRequestSinglePermission(android.Manifest.permission.CALL_PHONE)
                            }
                        )
                        PermissionStatusRow(
                            label = "Call Notifications",
                            granted = readiness.hasNotificationPermission,
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    onRequestSinglePermission(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                        if (readiness.isCallScreeningSupported) {
                            PermissionStatusRow(
                                label = "Call Screening System Role",
                                granted = readiness.hasCallScreeningRole,
                                onClick = onRequestCallScreeningRole
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onRequestPermissions,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("routing_manage_permissions_button")
                            ) {
                                Text("Manage Permissions")
                            }
                            if (readiness.isCallScreeningSupported) {
                                FilledTonalButton(
                                    onClick = onRequestCallScreeningRole,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Screening Role")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!granted) {
                Text(
                    text = "Grant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (granted) "Granted" else "Not granted, tap to grant",
                tint = if (granted) EmeraldLive else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
