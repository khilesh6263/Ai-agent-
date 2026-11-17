package com.example.ui

import android.Manifest
import android.app.role.RoleManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.PermissionFlowDialog
import com.example.ui.components.findActivity
import com.example.ui.components.isPermissionPermanentlyDenied
import com.example.ui.components.openAppPermissionSettings
import com.example.ui.screens.AgentDashboardScreen
import com.example.ui.screens.FaqsScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.LiveCallScreen
import com.example.ui.screens.RoutingScreen

private enum class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    AGENT("agent", R.string.nav_agent, Icons.Rounded.Shield, Icons.Outlined.Shield),
    INBOX("inbox", R.string.nav_inbox, Icons.Rounded.Inbox, Icons.Outlined.Inbox),
    FAQS("faqs", R.string.nav_faqs, Icons.Rounded.QuestionAnswer, Icons.Outlined.QuestionAnswer),
    ROUTING("routing", R.string.nav_routing, Icons.Rounded.AltRoute, Icons.Outlined.AltRoute)
}

@Composable
fun CallAgentApp(
    viewModel: CallAgentViewModel
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val faqs by viewModel.faqs.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredCallRecords.collectAsStateWithLifecycle()
    val allRecords by viewModel.allCallRecords.collectAsStateWithLifecycle()
    val inboxFilter by viewModel.inboxFilter.collectAsStateWithLifecycle()
    val liveCallState by viewModel.liveCallState.collectAsStateWithLifecycle()
    val readiness by viewModel.readiness.collectAsStateWithLifecycle()

    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var hasAttemptedPermissionPrompt by rememberSaveable { mutableStateOf(false) }
    var pendingMicListenAfterGrant by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshReadiness()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAttemptedPermissionPrompt = true
        viewModel.refreshReadiness()
        val anyCoreDenied = results.entries.any { (perm, granted) ->
            !granted && (
                perm == Manifest.permission.READ_PHONE_STATE ||
                    perm == Manifest.permission.ANSWER_PHONE_CALLS ||
                    perm == Manifest.permission.RECORD_AUDIO
                )
        }
        if (anyCoreDenied) {
            showPermissionDialog = true
        } else {
            showPermissionDialog = false
        }
    }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAttemptedPermissionPrompt = true
        viewModel.refreshReadiness()
        if (granted && pendingMicListenAfterGrant) {
            pendingMicListenAfterGrant = false
            viewModel.triggerMicListening()
        } else {
            pendingMicListenAfterGrant = false
            if (!granted) {
                showPermissionDialog = true
            }
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshReadiness()
    }

    val requestPermissionsBatch: (List<String>) -> Unit = { perms ->
        if (perms.isNotEmpty()) {
            multiPermissionLauncher.launch(perms.toTypedArray())
        }
    }

    val requestSinglePermission: (String) -> Unit = { perm ->
        val permanentlyDenied = isPermissionPermanentlyDenied(
            activity = activity,
            permission = perm,
            isGranted = false,
            hasAttemptedBefore = hasAttemptedPermissionPrompt
        )
        if (permanentlyDenied) {
            openAppPermissionSettings(context)
        } else {
            singlePermissionLauncher.launch(perm)
        }
    }

    val requestAudioInCall: () -> Unit = {
        val perm = Manifest.permission.RECORD_AUDIO
        val permanentlyDenied = isPermissionPermanentlyDenied(
            activity = activity,
            permission = perm,
            isGranted = readiness.hasRecordAudioPermission,
            hasAttemptedBefore = hasAttemptedPermissionPrompt
        )
        if (permanentlyDenied) {
            showPermissionDialog = true
        } else {
            pendingMicListenAfterGrant = true
            singlePermissionLauncher.launch(perm)
        }
    }

    val launchCallScreeningRole: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleLauncher.launch(intent)
            }
        }
    }

    AnimatedContent(
        targetState = liveCallState.isActive,
        label = "active_call_transition"
    ) { isInCall ->
        if (isInCall) {
            LiveCallScreen(
                liveState = liveCallState,
                agentName = settings.agentName,
                hasAudioPermission = readiness.hasRecordAudioPermission,
                onSendCallerInput = viewModel::sendSimulatedCallerInput,
                onTriggerMic = viewModel::triggerMicListening,
                onRequestAudioPermission = requestAudioInCall,
                onRedirectNow = viewModel::performManualRedirect,
                onEndCall = viewModel::endLiveCall
            )
        } else {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val navigateTop: (String) -> Unit = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                val useRail = maxWidth >= 600.dp

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                TopDestination.entries.forEach { dest ->
                                    val selected = currentDestination?.hierarchy?.any {
                                        it.route == dest.route
                                    } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTop(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(dest.labelRes)) },
                                        modifier = Modifier.testTag("nav_${dest.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                TopDestination.entries.forEach { dest ->
                                    val selected = currentDestination?.hierarchy?.any {
                                        it.route == dest.route
                                    } == true
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTop(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(dest.labelRes)) },
                                        modifier = Modifier.testTag("nav_rail_${dest.route}")
                                    )
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = TopDestination.AGENT.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(TopDestination.AGENT.route) {
                                AgentDashboardScreen(
                                    settings = settings,
                                    readiness = readiness,
                                    recentCalls = allRecords,
                                    onToggleAgent = viewModel::toggleAgentEnabled,
                                    onSelectAvailability = viewModel::setAvailability,
                                    onRequestPermissions = { showPermissionDialog = true },
                                    onRequestCallScreeningRole = launchCallScreeningRole,
                                    onSimulateCall = viewModel::startSimulatedCall,
                                    onOpenInbox = { navigateTop(TopDestination.INBOX.route) }
                                )
                            }
                            composable(TopDestination.INBOX.route) {
                                InboxScreen(
                                    records = filteredRecords,
                                    selectedFilter = inboxFilter,
                                    onSelectFilter = viewModel::setInboxFilter,
                                    onToggleResolved = viewModel::toggleCallResolved,
                                    onDeleteRecord = viewModel::deleteCallRecord
                                )
                            }
                            composable(TopDestination.FAQS.route) {
                                FaqsScreen(
                                    settings = settings,
                                    faqs = faqs,
                                    onSavePersona = viewModel::updateIdentityAndGreeting,
                                    onSaveFaq = viewModel::saveFaq,
                                    onToggleFaq = viewModel::toggleFaq,
                                    onDeleteFaq = viewModel::deleteFaq
                                )
                            }
                            composable(TopDestination.ROUTING.route) {
                                RoutingScreen(
                                    settings = settings,
                                    readiness = readiness,
                                    onSaveRouting = viewModel::updateRoutingSettings,
                                    onRequestPermissions = { showPermissionDialog = true },
                                    onRequestSinglePermission = requestSinglePermission,
                                    onRequestCallScreeningRole = launchCallScreeningRole
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPermissionDialog) {
        PermissionFlowDialog(
            readiness = readiness,
            hasAttemptedPrompt = hasAttemptedPermissionPrompt,
            activity = activity,
            onDismiss = { showPermissionDialog = false },
            onRequestMissingAll = requestPermissionsBatch,
            onRequestSingle = requestSinglePermission,
            onRequestCallScreeningRole = launchCallScreeningRole,
            onOpenSettings = { openAppPermissionSettings(context) }
        )
    }
}
