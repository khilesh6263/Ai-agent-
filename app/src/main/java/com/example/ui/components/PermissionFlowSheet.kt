package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhoneForwarded
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.data.SystemReadiness
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.EmeraldLive

data class PermissionSpec(
    val permission: String,
    val title: String,
    val rationale: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val isRequiredForCore: Boolean
)

fun buildPermissionSpecs(readiness: SystemReadiness): List<PermissionSpec> = buildList {
    add(
        PermissionSpec(
            permission = Manifest.permission.READ_PHONE_STATE,
            title = "Incoming Call State",
            rationale = "Detects when your phone rings, connects, or ends a call.",
            icon = Icons.Rounded.Call,
            isGranted = readiness.hasPhoneStatePermission,
            isRequiredForCore = true
        )
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        add(
            PermissionSpec(
                permission = Manifest.permission.ANSWER_PHONE_CALLS,
                title = "Auto-Answer Calls",
                rationale = "Allows the AI agent to automatically pick up incoming calls when you're busy.",
                icon = Icons.Rounded.PhoneInTalk,
                isGranted = readiness.hasAnswerCallsPermission,
                isRequiredForCore = true
            )
        )
    }
    add(
        PermissionSpec(
            permission = Manifest.permission.RECORD_AUDIO,
            title = "Caller Audio & Voice",
            rationale = "Listens to caller speech and enables live two-way voice responses.",
            icon = Icons.Rounded.Mic,
            isGranted = readiness.hasRecordAudioPermission,
            isRequiredForCore = true
        )
    )
    add(
        PermissionSpec(
            permission = Manifest.permission.CALL_PHONE,
            title = "Call Redirect Dialing",
            rationale = "Transfers urgent or requested calls to your redirect contact.",
            icon = Icons.Rounded.PhoneForwarded,
            isGranted = readiness.hasCallPhonePermission,
            isRequiredForCore = false
        )
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(
            PermissionSpec(
                permission = Manifest.permission.POST_NOTIFICATIONS,
                title = "Active Call Alerts",
                rationale = "Displays ongoing controls while the AI agent is speaking with a caller.",
                icon = Icons.Rounded.NotificationsActive,
                isGranted = readiness.hasNotificationPermission,
                isRequiredForCore = false
            )
        )
    }
}

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

fun isPermissionPermanentlyDenied(
    activity: Activity?,
    permission: String,
    isGranted: Boolean,
    hasAttemptedBefore: Boolean
): Boolean {
    if (isGranted || !hasAttemptedBefore || activity == null) return false
    return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

fun openAppPermissionSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

@Composable
fun PermissionFlowDialog(
    readiness: SystemReadiness,
    hasAttemptedPrompt: Boolean,
    activity: Activity?,
    onDismiss: () -> Unit,
    onRequestMissingAll: (List<String>) -> Unit,
    onRequestSingle: (String) -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val specs = buildPermissionSpecs(readiness)
    val missingPermissions = specs.filter { !it.isGranted }.map { it.permission }
    val anyPermanentlyDenied = specs.any {
        isPermissionPermanentlyDenied(
            activity = activity,
            permission = it.permission,
            isGranted = it.isGranted,
            hasAttemptedBefore = hasAttemptedPrompt
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Call & Audio Permissions",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Grant access so CallAgent can detect incoming calls, answer automatically, and handle live caller audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                specs.forEach { spec ->
                    val permanentlyDenied = isPermissionPermanentlyDenied(
                        activity = activity,
                        permission = spec.permission,
                        isGranted = spec.isGranted,
                        hasAttemptedBefore = hasAttemptedPrompt
                    )
                    PermissionSpecRow(
                        spec = spec,
                        permanentlyDenied = permanentlyDenied,
                        onAllowClick = {
                            if (permanentlyDenied) {
                                onOpenSettings()
                            } else {
                                onRequestSingle(spec.permission)
                            }
                        }
                    )
                }

                if (readiness.isCallScreeningSupported) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(
                            1.dp,
                            if (readiness.hasCallScreeningRole) {
                                EmeraldLive.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = if (readiness.hasCallScreeningRole) {
                                    EmeraldLive
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Call Screening Role",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Identifies incoming callers and silences ringing before AI answers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (readiness.hasCallScreeningRole) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Enabled",
                                    tint = EmeraldLive,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                TextButton(
                                    onClick = onRequestCallScreeningRole,
                                    modifier = Modifier.testTag("dialog_screening_role_button")
                                ) {
                                    Text("Enable")
                                }
                            }
                        }
                    }
                }

                if (anyPermanentlyDenied) {
                    Surface(
                        color = AmberAlert.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, AmberAlert.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Some permissions were previously denied. Open App Settings to enable them manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_app_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open App Settings")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (missingPermissions.isNotEmpty()) {
                Button(
                    onClick = {
                        if (anyPermanentlyDenied && missingPermissions.all {
                                isPermissionPermanentlyDenied(activity, it, false, hasAttemptedPrompt)
                            }
                        ) {
                            onOpenSettings()
                        } else {
                            onRequestMissingAll(missingPermissions)
                        }
                    },
                    modifier = Modifier.testTag("dialog_grant_all_permissions_button")
                ) {
                    Text(if (anyPermanentlyDenied) "Continue" else "Allow All")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (missingPermissions.isEmpty()) "Close" else "Later")
            }
        }
    )
}

@Composable
private fun PermissionSpecRow(
    spec: PermissionSpec,
    permanentlyDenied: Boolean,
    onAllowClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            1.dp,
            if (spec.isGranted) EmeraldLive.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                tint = if (spec.isGranted) EmeraldLive else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = spec.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (spec.isRequiredForCore && !spec.isGranted) {
                        Text(
                            text = "REQUIRED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberAlert
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = spec.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (spec.isGranted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Granted",
                    tint = EmeraldLive,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                TextButton(
                    onClick = onAllowClick,
                    modifier = Modifier.testTag("grant_single_${spec.permission.substringAfterLast('.').lowercase()}")
                ) {
                    Text(if (permanentlyDenied) "Settings" else "Allow")
                }
            }
        }
    }
}
