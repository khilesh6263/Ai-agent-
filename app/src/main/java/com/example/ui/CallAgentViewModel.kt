package com.example.ui

import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AgentSettings
import com.example.data.AvailabilityStatus
import com.example.data.CallAgentRepository
import com.example.data.CallOutcome
import com.example.data.CallRecord
import com.example.data.FaqItem
import com.example.data.LanguageMode
import com.example.data.LiveCallState
import com.example.data.RedirectMode
import com.example.data.SystemReadiness
import com.example.data.UrgencyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InboxFilter(val label: String) {
    ALL("All Calls"),
    MESSAGES("Messages"),
    REDIRECTED("Redirected"),
    URGENT("Urgent")
}

class CallAgentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CallAgentRepository(application)

    val settings: StateFlow<AgentSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AgentSettings()
        )

    val faqs: StateFlow<List<FaqItem>> = repository.faqsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _inboxFilter = MutableStateFlow(InboxFilter.ALL)
    val inboxFilter: StateFlow<InboxFilter> = _inboxFilter.asStateFlow()

    val allCallRecords: StateFlow<List<CallRecord>> = repository.callRecordsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val filteredCallRecords: StateFlow<List<CallRecord>> = combine(
        repository.callRecordsFlow,
        _inboxFilter
    ) { records, filter ->
        when (filter) {
            InboxFilter.ALL -> records
            InboxFilter.MESSAGES -> records.filter {
                it.outcome == CallOutcome.MESSAGE_TAKEN || it.capturedMessage.isNotBlank()
            }
            InboxFilter.REDIRECTED -> records.filter { it.outcome == CallOutcome.REDIRECTED }
            InboxFilter.URGENT -> records.filter {
                it.urgency == UrgencyLevel.HIGH || it.urgency == UrgencyLevel.URGENT
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val liveCallState: StateFlow<LiveCallState> = repository.liveCallState

    private val _readiness = MutableStateFlow(computeSystemReadiness(application))
    val readiness: StateFlow<SystemReadiness> = _readiness.asStateFlow()

    fun refreshReadiness() {
        _readiness.value = computeSystemReadiness(getApplication())
    }

    fun setInboxFilter(filter: InboxFilter) {
        _inboxFilter.value = filter
    }

    fun toggleAgentEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(isAgentEnabled = enabled) }
        }
    }

    fun setAvailability(status: AvailabilityStatus) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(availabilityStatus = status) }
        }
    }

    fun updateIdentityAndGreeting(
        agentName: String,
        ownerName: String,
        availabilityNote: String,
        customGreeting: String
    ) {
        viewModelScope.launch {
            repository.updateSettings {
                it.copy(
                    agentName = agentName.ifBlank { "Aria" },
                    ownerName = ownerName.ifBlank { "Alex" },
                    availabilityNote = availabilityNote,
                    customGreeting = customGreeting
                )
            }
        }
    }

    fun updateRoutingSettings(
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
    ) {
        viewModelScope.launch {
            repository.updateSettings {
                it.copy(
                    autoAnswerWhenAvailable = autoAnswerWhenAvailable,
                    autoAnswerDelaySeconds = autoAnswerDelaySeconds.coerceIn(0, 15),
                    languageMode = languageMode,
                    redirectEnabled = redirectEnabled,
                    redirectTargetName = redirectTargetName.ifBlank { "Team Desk" },
                    redirectPhoneNumber = redirectPhoneNumber.trim(),
                    redirectMode = redirectMode,
                    redirectOnUrgent = redirectOnUrgent,
                    redirectOnRequest = redirectOnRequest,
                    vipNumbersRaw = vipNumbersRaw
                )
            }
        }
    }

    fun saveFaq(item: FaqItem) {
        viewModelScope.launch { repository.saveFaq(item) }
    }

    fun toggleFaq(item: FaqItem) {
        viewModelScope.launch { repository.toggleFaqEnabled(item) }
    }

    fun deleteFaq(id: Long) {
        viewModelScope.launch { repository.deleteFaq(id) }
    }

    fun toggleCallResolved(record: CallRecord) {
        viewModelScope.launch {
            repository.setCallResolved(record.id, !record.isResolved)
        }
    }

    fun deleteCallRecord(id: Long) {
        viewModelScope.launch { repository.deleteCallRecord(id) }
    }

    fun startSimulatedCall(callerName: String, callerNumber: String) {
        repository.startSimulatedCall(callerName, callerNumber)
    }

    fun sendSimulatedCallerInput(text: String) {
        repository.sendSimulatedCallerMessage(text, speakAloud = true)
    }

    fun triggerMicListening() {
        repository.triggerMicListening()
    }

    fun performManualRedirect() {
        repository.performRedirect()
    }

    fun endLiveCall() {
        repository.endLiveCall()
    }

    private fun computeSystemReadiness(context: Context): SystemReadiness {
        fun granted(perm: String) = ContextCompat.checkSelfPermission(
            context,
            perm
        ) == PackageManager.PERMISSION_GRANTED

        val hasPhoneState = granted(Manifest.permission.READ_PHONE_STATE)
        val hasAnswerCalls = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            granted(Manifest.permission.ANSWER_PHONE_CALLS)
        } else {
            true
        }
        val hasRecordAudio = granted(Manifest.permission.RECORD_AUDIO)
        val hasCallPhone = granted(Manifest.permission.CALL_PHONE)
        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

        var isScreeningSupported = false
        var hasScreeningRole = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null) {
                isScreeningSupported = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                hasScreeningRole = isScreeningSupported &&
                    roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            }
        }

        return SystemReadiness(
            hasPhoneStatePermission = hasPhoneState,
            hasAnswerCallsPermission = hasAnswerCalls,
            hasRecordAudioPermission = hasRecordAudio,
            hasCallPhonePermission = hasCallPhone,
            hasNotificationPermission = hasNotifications,
            hasCallScreeningRole = hasScreeningRole,
            isCallScreeningSupported = isScreeningSupported
        )
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return CallAgentViewModel(application) as T
                }
            }
    }
}
