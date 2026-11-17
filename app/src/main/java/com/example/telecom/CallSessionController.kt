package com.example.telecom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.ai.AiCallOrchestrator
import com.example.data.AgentSettings
import com.example.data.AgentTurnDecision
import com.example.data.CallOutcome
import com.example.data.CallPhase
import com.example.data.CallRecord
import com.example.data.FaqItem
import com.example.data.LiveCallState
import com.example.data.RedirectMode
import com.example.data.SpeakerRole
import com.example.data.TranscriptMessage
import com.example.data.UrgencyLevel
import com.example.data.local.AppDatabase
import com.example.data.local.CallRecordEntity
import com.example.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "CallSessionController"

class CallSessionController private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val orchestrator = AiCallOrchestrator()
    private val voiceBridge = VoiceBridge(appContext)
    private val database = AppDatabase.getInstance(appContext)
    private val settingsStore = SettingsDataStore(appContext)

    private val _liveState = MutableStateFlow(LiveCallState())
    val liveState: StateFlow<LiveCallState> = _liveState.asStateFlow()

    private var sessionJob: Job? = null
    private var timerJob: Job? = null
    private var voiceLoopJob: Job? = null
    private var sessionStartedAtMillis: Long = 0L
    private var currentSettings: AgentSettings = AgentSettings()
    private var currentFaqs: List<FaqItem> = emptyList()
    private var hasSavedCurrentCall = false

    fun startIncomingAiSession(
        callerName: String,
        callerNumber: String,
        isSimulated: Boolean,
        autoVoiceListen: Boolean = true
    ) {
        if (_liveState.value.isActive) {
            Log.w(TAG, "Call session already active; ignoring duplicate start.")
            return
        }

        sessionJob?.cancel()
        timerJob?.cancel()
        voiceLoopJob?.cancel()
        hasSavedCurrentCall = false
        sessionStartedAtMillis = System.currentTimeMillis()

        sessionJob = scope.launch {
            currentSettings = settingsStore.getSnapshot()
            currentFaqs = database.faqDao().getEnabledOnce().map { it.toDomain() }

            val displayCaller = callerName.ifBlank { "Unknown Caller" }
            val displayNumber = callerNumber.ifBlank { "Private Number" }

            _liveState.value = LiveCallState(
                isActive = true,
                isSimulated = isSimulated,
                callerName = displayCaller,
                callerNumber = displayNumber,
                phase = if (isSimulated) CallPhase.RINGING_COUNTDOWN else CallPhase.GREETING,
                countdownSeconds = if (isSimulated) currentSettings.autoAnswerDelaySeconds.coerceAtLeast(1) else 0,
                statusBanner = if (isSimulated) {
                    "Incoming simulated call — auto-answering..."
                } else {
                    "AI Agent answered call"
                }
            )

            if (isSimulated) {
                for (sec in currentSettings.autoAnswerDelaySeconds.coerceAtLeast(1) downTo 1) {
                    if (!_liveState.value.isActive) return@launch
                    _liveState.update {
                        it.copy(
                            countdownSeconds = sec,
                            statusBanner = "Auto-answering in ${sec}s..."
                        )
                    }
                    delay(1000L)
                }
            }

            if (!_liveState.value.isActive) return@launch

            startElapsedTimer()
            if (!isSimulated) {
                voiceBridge.enableCallSpeakerphone(true)
            }

            _liveState.update {
                it.copy(
                    phase = CallPhase.GREETING,
                    countdownSeconds = 0,
                    statusBanner = "${currentSettings.agentName} is greeting caller..."
                )
            }

            val greetingResult = orchestrator.generateOpeningGreeting(
                settings = currentSettings,
                callerName = displayCaller,
                callerNumber = displayNumber
            )

            greetingResult.fold(
                onSuccess = { decision ->
                    applyAgentDecision(decision, speakAloud = true)
                    if (autoVoiceListen && hasAudioPermission()) {
                        startContinuousListeningLoop()
                    } else {
                        _liveState.update {
                            it.copy(
                                phase = CallPhase.LISTENING,
                                statusBanner = if (hasAudioPermission()) {
                                    "Tap microphone or send a caller reply below"
                                } else {
                                    "Mic permission needed for voice; type or tap a caller prompt below"
                                }
                            )
                        }
                    }
                },
                onFailure = { err ->
                    val errMsg = err.localizedMessage ?: "Failed to connect to Gemini AI"
                    Log.e(TAG, "Greeting generation failed: $errMsg", err)
                    _liveState.update {
                        it.copy(
                            phase = CallPhase.LISTENING,
                            aiError = errMsg,
                            statusBanner = "AI connection error"
                        )
                    }
                }
            )
        }
    }

    fun submitCallerInput(utterance: String, speakReplyAloud: Boolean = true) {
        val clean = utterance.trim()
        if (clean.isEmpty() || !_liveState.value.isActive) return

        voiceBridge.stopListening()
        voiceLoopJob?.cancel()

        scope.launch {
            handleSingleCallerTurn(clean, speakReplyAloud)
        }
    }

    fun triggerVoiceCaptureOnce() {
        if (!_liveState.value.isActive) return
        if (!hasAudioPermission()) {
            _liveState.update {
                it.copy(aiError = "Microphone permission is required for voice capture.")
            }
            return
        }

        voiceLoopJob?.cancel()
        voiceLoopJob = scope.launch {
            _liveState.update {
                it.copy(
                    phase = CallPhase.LISTENING,
                    isListeningAudio = true,
                    aiError = null,
                    statusBanner = "Listening to caller..."
                )
            }
            val listenResult = voiceBridge.listenOnce(currentSettings.languageMode)
            _liveState.update { it.copy(isListeningAudio = false) }
            listenResult.fold(
                onSuccess = { spoken ->
                    handleSingleCallerTurn(spoken, speakReplyAloud = true)
                },
                onFailure = { err ->
                    _liveState.update {
                        it.copy(
                            phase = CallPhase.LISTENING,
                            statusBanner = err.message ?: "Didn't catch that — try mic or quick reply"
                        )
                    }
                }
            )
        }
    }

    private fun startContinuousListeningLoop() {
        voiceLoopJob?.cancel()
        voiceLoopJob = scope.launch {
            while (_liveState.value.isActive &&
                _liveState.value.phase != CallPhase.ENDED &&
                _liveState.value.phase != CallPhase.REDIRECTING
            ) {
                _liveState.update {
                    it.copy(
                        phase = CallPhase.LISTENING,
                        isListeningAudio = true,
                        statusBanner = "Listening to caller..."
                    )
                }
                val result = voiceBridge.listenOnce(currentSettings.languageMode)
                _liveState.update { it.copy(isListeningAudio = false) }

                val spoken = result.getOrNull()
                if (!spoken.isNullOrBlank()) {
                    val shouldContinue = handleSingleCallerTurn(spoken, speakReplyAloud = true)
                    if (!shouldContinue) break
                } else {
                    _liveState.update {
                        it.copy(
                            phase = CallPhase.LISTENING,
                            statusBanner = "Ready — tap Mic or send caller message"
                        )
                    }
                    break
                }
            }
        }
    }

    private suspend fun handleSingleCallerTurn(
        callerText: String,
        speakReplyAloud: Boolean
    ): Boolean {
        val stateBefore = _liveState.value
        if (!stateBefore.isActive) return false

        val callerMsg = TranscriptMessage(
            id = UUID.randomUUID().toString(),
            speaker = SpeakerRole.CALLER,
            text = callerText
        )

        _liveState.update {
            it.copy(
                transcript = it.transcript + callerMsg,
                phase = CallPhase.THINKING,
                aiError = null,
                statusBanner = "${currentSettings.agentName} is thinking..."
            )
        }

        val currentHistory = _liveState.value.transcript
        val result = orchestrator.processCallerTurn(
            settings = currentSettings,
            faqs = currentFaqs,
            callerName = stateBefore.callerName,
            callerNumber = stateBefore.callerNumber,
            history = currentHistory,
            callerUtterance = callerText
        )

        return result.fold(
            onSuccess = { decision ->
                applyAgentDecision(decision, speakReplyAloud)
                when {
                    decision.shouldRedirect -> {
                        executeRedirectIfConfigured(decision)
                        false
                    }
                    decision.shouldEndCall -> {
                        delay(800L)
                        endCallSession(CallOutcome.MESSAGE_TAKEN)
                        false
                    }
                    else -> {
                        _liveState.update {
                            it.copy(
                                phase = CallPhase.LISTENING,
                                statusBanner = "Awaiting caller response"
                            )
                        }
                        true
                    }
                }
            },
            onFailure = { err ->
                val message = err.localizedMessage ?: "AI processing failed"
                Log.e(TAG, "AI turn failed: $message", err)
                _liveState.update {
                    it.copy(
                        phase = CallPhase.LISTENING,
                        aiError = message,
                        statusBanner = "AI error — retry or take over manually"
                    )
                }
                false
            }
        )
    }

    private suspend fun applyAgentDecision(decision: AgentTurnDecision, speakAloud: Boolean) {
        val agentMsg = TranscriptMessage(
            id = UUID.randomUUID().toString(),
            speaker = SpeakerRole.AGENT,
            text = decision.spokenReply,
            intentTag = decision.intent
        )

        _liveState.update { current ->
            val mergedUrgency = if (decision.urgency.ordinal > current.urgency.ordinal) {
                decision.urgency
            } else {
                current.urgency
            }
            val updatedCallerName = decision.capturedCallerName?.takeIf { it.isNotBlank() }
                ?: current.capturedCallerName
            val updatedMessage = decision.capturedMessage?.takeIf { it.isNotBlank() }
                ?: current.capturedMessage

            current.copy(
                transcript = current.transcript + agentMsg,
                currentIntent = decision.intent,
                urgency = mergedUrgency,
                capturedCallerName = updatedCallerName,
                capturedMessage = updatedMessage,
                redirectRequested = current.redirectRequested || decision.shouldRedirect,
                redirectTargetName = if (decision.shouldRedirect) currentSettings.redirectTargetName else current.redirectTargetName,
                redirectTargetNumber = if (decision.shouldRedirect) currentSettings.redirectPhoneNumber else current.redirectTargetNumber,
                redirectReason = decision.redirectReason ?: current.redirectReason,
                phase = CallPhase.SPEAKING,
                isSpeakingAudio = speakAloud,
                statusBanner = "${currentSettings.agentName} is speaking..."
            )
        }

        if (speakAloud) {
            voiceBridge.speak(decision.spokenReply, currentSettings.languageMode)
            _liveState.update { it.copy(isSpeakingAudio = false) }
        }
    }

    private suspend fun executeRedirectIfConfigured(decision: AgentTurnDecision) {
        val targetNumber = currentSettings.redirectPhoneNumber.trim()
        val systemNote = if (currentSettings.redirectEnabled && targetNumber.isNotEmpty()) {
            "Redirecting call to ${currentSettings.redirectTargetName} ($targetNumber)..."
        } else {
            "Redirect flagged (${decision.redirectReason ?: "Urgent"}), awaiting user action."
        }

        _liveState.update {
            it.copy(
                phase = CallPhase.REDIRECTING,
                transcript = it.transcript + TranscriptMessage(
                    id = UUID.randomUUID().toString(),
                    speaker = SpeakerRole.SYSTEM,
                    text = systemNote,
                    intentTag = "REDIRECT"
                ),
                statusBanner = systemNote
            )
        }

        if (currentSettings.redirectEnabled &&
            targetNumber.isNotEmpty() &&
            currentSettings.redirectMode == RedirectMode.AUTO_TRANSFER &&
            !_liveState.value.isSimulated
        ) {
            launchRedirectDial(targetNumber)
        }

        delay(1200L)
        endCallSession(CallOutcome.REDIRECTED)
    }

    fun performManualRedirect() {
        val targetNumber = currentSettings.redirectPhoneNumber.trim()
        if (targetNumber.isNotEmpty()) {
            launchRedirectDial(targetNumber)
        }
        scope.launch {
            _liveState.update {
                it.copy(
                    redirectRequested = true,
                    redirectTargetName = currentSettings.redirectTargetName,
                    redirectTargetNumber = targetNumber,
                    transcript = it.transcript + TranscriptMessage(
                        id = UUID.randomUUID().toString(),
                        speaker = SpeakerRole.SYSTEM,
                        text = "Call redirected to ${currentSettings.redirectTargetName} ${if (targetNumber.isNotEmpty()) "($targetNumber)" else ""}".trim(),
                        intentTag = "REDIRECT"
                    )
                )
            }
            endCallSession(CallOutcome.REDIRECTED)
        }
    }

    private fun launchRedirectDial(phoneNumber: String) {
        val hasCallPerm = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val action = if (hasCallPerm) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val intent = Intent(action, Uri.parse("tel:${Uri.encode(phoneNumber)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
            .onFailure { Log.e(TAG, "Failed to launch redirect call intent", it) }
    }

    fun endCallSession(explicitOutcome: CallOutcome? = null) {
        voiceLoopJob?.cancel()
        sessionJob?.cancel()
        timerJob?.cancel()
        voiceBridge.stopSpeaking()
        voiceBridge.stopListening()
        voiceBridge.enableCallSpeakerphone(false)

        val snapshot = _liveState.value
        if (!snapshot.isActive && hasSavedCurrentCall) {
            _liveState.value = LiveCallState()
            return
        }

        val resolvedOutcome = explicitOutcome ?: when {
            snapshot.redirectRequested -> CallOutcome.REDIRECTED
            !snapshot.capturedMessage.isNullOrBlank() -> CallOutcome.MESSAGE_TAKEN
            snapshot.currentIntent == "FAQ_ANSWERED" -> CallOutcome.FAQ_ANSWERED
            snapshot.transcript.any { it.speaker == SpeakerRole.AGENT } -> CallOutcome.GREETED
            else -> CallOutcome.MISSED_OR_ENDED
        }

        if (snapshot.isActive && !hasSavedCurrentCall) {
            hasSavedCurrentCall = true
            val record = CallRecord(
                callerName = snapshot.capturedCallerName ?: snapshot.callerName,
                callerNumber = snapshot.callerNumber,
                startedAtMillis = sessionStartedAtMillis,
                durationSeconds = snapshot.elapsedSeconds.coerceAtLeast(1),
                availabilityAtCall = currentSettings.availabilityStatus,
                outcome = resolvedOutcome,
                urgency = snapshot.urgency,
                capturedMessage = snapshot.capturedMessage ?: buildFallbackSummary(snapshot.transcript),
                redirectTarget = if (resolvedOutcome == CallOutcome.REDIRECTED) {
                    snapshot.redirectTargetName ?: currentSettings.redirectTargetName
                } else null,
                isSimulated = snapshot.isSimulated,
                isResolved = resolvedOutcome == CallOutcome.FAQ_ANSWERED || resolvedOutcome == CallOutcome.REDIRECTED,
                transcript = snapshot.transcript
            )
            scope.launch(Dispatchers.IO) {
                database.callRecordDao().insert(CallRecordEntity.fromDomain(record))
            }
        }

        _liveState.value = LiveCallState()
        AiCallAgentService.stop(appContext)
    }

    private fun buildFallbackSummary(transcript: List<TranscriptMessage>): String {
        val callerLines = transcript.filter { it.speaker == SpeakerRole.CALLER }.map { it.text }
        return if (callerLines.isNotEmpty()) {
            callerLines.joinToString(" / ").take(180)
        } else {
            "AI greeted caller; no message left."
        }
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                _liveState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        @Volatile
        private var INSTANCE: CallSessionController? = null

        fun getInstance(context: Context): CallSessionController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallSessionController(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
