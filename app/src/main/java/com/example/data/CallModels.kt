package com.example.data

enum class AvailabilityStatus(
    val label: String,
    val description: String,
    val autoAnswerRecommended: Boolean
) {
    AVAILABLE(
        label = "Available",
        description = "Calls ring normally unless you enable always-on screening",
        autoAnswerRecommended = false
    ),
    BUSY(
        label = "Busy",
        description = "AI answers, handles FAQs, checks urgency, and takes messages",
        autoAnswerRecommended = true
    ),
    IN_MEETING(
        label = "In Meeting",
        description = "AI informs callers you are in a meeting and offers message or redirect",
        autoAnswerRecommended = true
    ),
    AWAY(
        label = "Away / DND",
        description = "AI answers immediately, collects callback details, and guards your focus",
        autoAnswerRecommended = true
    )
}

enum class LanguageMode(val label: String, val instructionHint: String) {
    AUTO_BILINGUAL(
        label = "Auto (English + Hindi / Hinglish)",
        instructionHint = "Respond naturally in the same language the caller uses (English, Hindi, or Hinglish)."
    ),
    ENGLISH(
        label = "English Only",
        instructionHint = "Respond in clear, polite conversational English."
    ),
    HINDI(
        label = "Hindi (हिंदी)",
        instructionHint = "Respond in polite, natural conversational Hindi (Devanagari or simple spoken Hindi)."
    )
}

enum class RedirectMode(val label: String, val description: String) {
    AUTO_TRANSFER(
        label = "Auto-Transfer Call",
        description = "Automatically places a transfer call to your redirect contact when urgent or requested"
    ),
    ALERT_USER(
        label = "Prompt & Ring User",
        description = "Notifies you immediately with a one-tap redirect option instead of dialing out"
    )
}

enum class CallOutcome(val label: String) {
    GREETED("Greeted"),
    MESSAGE_TAKEN("Message Taken"),
    FAQ_ANSWERED("FAQ Answered"),
    REDIRECTED("Redirected"),
    USER_AVAILABLE("Connected"),
    MISSED_OR_ENDED("Ended")
}

enum class UrgencyLevel(val label: String) {
    LOW("Low"),
    MEDIUM("Normal"),
    HIGH("High"),
    URGENT("Urgent")
}

data class AgentSettings(
    val isAgentEnabled: Boolean = true,
    val agentName: String = "Aria",
    val ownerName: String = "Alex",
    val availabilityStatus: AvailabilityStatus = AvailabilityStatus.BUSY,
    val availabilityNote: String = "In a focus session until 4:00 PM",
    val customGreeting: String = "",
    val autoAnswerWhenAvailable: Boolean = false,
    val autoAnswerDelaySeconds: Int = 3,
    val languageMode: LanguageMode = LanguageMode.AUTO_BILINGUAL,
    val redirectEnabled: Boolean = true,
    val redirectTargetName: String = "Team Desk",
    val redirectPhoneNumber: String = "",
    val redirectMode: RedirectMode = RedirectMode.ALERT_USER,
    val redirectOnUrgent: Boolean = true,
    val redirectOnRequest: Boolean = true,
    val vipNumbersRaw: String = ""
) {
    val shouldAutoAnswerCalls: Boolean
        get() = isAgentEnabled && (availabilityStatus.autoAnswerRecommended || autoAnswerWhenAvailable)

    fun isVipNumber(number: String): Boolean {
        val normalized = number.filter { it.isDigit() }
        if (normalized.length < 4) return false
        return vipNumbersRaw
            .split(",", "\n")
            .map { it.trim().filter { ch -> ch.isDigit() } }
            .filter { it.length >= 4 }
            .any { vip -> normalized.endsWith(vip) || vip.endsWith(normalized) }
    }
}

data class FaqItem(
    val id: Long = 0,
    val question: String,
    val answer: String,
    val category: String = "General",
    val isEnabled: Boolean = true
)

data class TranscriptMessage(
    val id: String,
    val speaker: SpeakerRole,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val intentTag: String? = null
)

enum class SpeakerRole {
    AGENT,
    CALLER,
    SYSTEM
}

data class CallRecord(
    val id: Long = 0,
    val callerName: String,
    val callerNumber: String,
    val startedAtMillis: Long,
    val durationSeconds: Int,
    val availabilityAtCall: AvailabilityStatus,
    val outcome: CallOutcome,
    val urgency: UrgencyLevel,
    val capturedMessage: String,
    val redirectTarget: String?,
    val isSimulated: Boolean,
    val isResolved: Boolean = false,
    val transcript: List<TranscriptMessage> = emptyList()
)

data class AgentTurnDecision(
    val spokenReply: String,
    val intent: String,
    val urgency: UrgencyLevel,
    val shouldRedirect: Boolean,
    val redirectReason: String?,
    val capturedCallerName: String?,
    val capturedMessage: String?,
    val shouldEndCall: Boolean
)

enum class CallPhase {
    IDLE,
    RINGING_COUNTDOWN,
    GREETING,
    LISTENING,
    THINKING,
    SPEAKING,
    REDIRECTING,
    ENDED
}

data class LiveCallState(
    val isActive: Boolean = false,
    val isSimulated: Boolean = false,
    val callerName: String = "",
    val callerNumber: String = "",
    val phase: CallPhase = CallPhase.IDLE,
    val countdownSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val transcript: List<TranscriptMessage> = emptyList(),
    val currentIntent: String = "STANDBY",
    val urgency: UrgencyLevel = UrgencyLevel.LOW,
    val capturedCallerName: String? = null,
    val capturedMessage: String? = null,
    val redirectRequested: Boolean = false,
    val redirectTargetNumber: String? = null,
    val redirectTargetName: String? = null,
    val redirectReason: String? = null,
    val isListeningAudio: Boolean = false,
    val isSpeakingAudio: Boolean = false,
    val statusBanner: String = "",
    val aiError: String? = null
)

data class SystemReadiness(
    val hasPhoneStatePermission: Boolean = false,
    val hasAnswerCallsPermission: Boolean = false,
    val hasRecordAudioPermission: Boolean = false,
    val hasCallPhonePermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasCallScreeningRole: Boolean = false,
    val isCallScreeningSupported: Boolean = false
) {
    val isCoreReady: Boolean
        get() = hasPhoneStatePermission && hasAnswerCallsPermission && hasRecordAudioPermission
}
