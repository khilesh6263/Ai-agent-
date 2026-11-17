package com.example.ai

import android.util.Log
import com.example.data.AgentSettings
import com.example.data.AgentTurnDecision
import com.example.data.FaqItem
import com.example.data.SpeakerRole
import com.example.data.TranscriptMessage
import com.example.data.UrgencyLevel
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiCallOrchestrator"

class AiCallOrchestrator {

    private val decisionSchema = Schema.obj(
        properties = mapOf(
            "spokenReply" to Schema.string(
                description = "Natural, polite, concise spoken response for the caller (1-3 sentences, no markdown or emojis)."
            ),
            "intent" to Schema.enumeration(
                values = listOf(
                    "GREETING",
                    "CHECK_AVAILABILITY",
                    "FAQ_ANSWERED",
                    "TAKE_MESSAGE",
                    "REDIRECT_REQUESTED",
                    "GOODBYE"
                ),
                description = "Primary intent of this conversational turn."
            ),
            "urgency" to Schema.enumeration(
                values = listOf("LOW", "MEDIUM", "HIGH", "URGENT"),
                description = "Estimated urgency of the caller's request."
            ),
            "shouldRedirect" to Schema.boolean(
                description = "True only if redirect is enabled and the caller either needs urgent escalation or explicitly asked to be transferred/redirected."
            ),
            "redirectReason" to Schema.string(
                description = "Brief reason for redirect if shouldRedirect is true, otherwise empty string.",
                nullable = true
            ),
            "capturedCallerName" to Schema.string(
                description = "Caller's name if mentioned in conversation, otherwise null.",
                nullable = true
            ),
            "capturedMessage" to Schema.string(
                description = "Concise summary of the caller's message or callback request if provided, otherwise null.",
                nullable = true
            ),
            "shouldEndCall" to Schema.boolean(
                description = "True when the caller has finished leaving their message or said goodbye."
            )
        ),
        optionalProperties = listOf("redirectReason", "capturedCallerName", "capturedMessage")
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.4f
                responseMimeType = "application/json"
                responseSchema = decisionSchema
            }
        )
    }

    suspend fun generateOpeningGreeting(
        settings: AgentSettings,
        callerName: String,
        callerNumber: String
    ): Result<AgentTurnDecision> {
        val custom = settings.customGreeting.trim()
        if (custom.isNotEmpty()) {
            val personalized = custom
                .replace("{agent}", settings.agentName)
                .replace("{owner}", settings.ownerName)
                .replace("{status}", settings.availabilityStatus.label)
            return Result.success(
                AgentTurnDecision(
                    spokenReply = personalized,
                    intent = "GREETING",
                    urgency = UrgencyLevel.LOW,
                    shouldRedirect = false,
                    redirectReason = null,
                    capturedCallerName = callerName.takeIf { it != "Unknown Caller" && it.isNotBlank() },
                    capturedMessage = null,
                    shouldEndCall = false
                )
            )
        }

        val prompt = buildString {
            appendLine(buildContextBlock(settings, emptyList(), callerName, callerNumber))
            appendLine()
            appendLine("TASK: Generate a warm, natural 1-2 sentence opening greeting to answer this incoming phone call.")
            appendLine("Introduce yourself as ${settings.agentName}, ${settings.ownerName}'s AI call assistant.")
            appendLine("Mention ${settings.ownerName}'s current status (${settings.availabilityStatus.label}) and politely ask who is calling or how you can help (answer a question, take a message, or redirect if urgent).")
            appendLine("Keep it brief like a real phone greeting. Output raw JSON matching the schema.")
        }

        return invokeStructuredTurn(prompt)
    }

    suspend fun processCallerTurn(
        settings: AgentSettings,
        faqs: List<FaqItem>,
        callerName: String,
        callerNumber: String,
        history: List<TranscriptMessage>,
        callerUtterance: String
    ): Result<AgentTurnDecision> {
        val prompt = buildString {
            appendLine(buildContextBlock(settings, faqs, callerName, callerNumber))
            appendLine()
            appendLine("CONVERSATION SO FAR:")
            history.takeLast(12).forEach { msg ->
                val roleLabel = when (msg.speaker) {
                    SpeakerRole.AGENT -> "AI Agent (${settings.agentName})"
                    SpeakerRole.CALLER -> "Caller"
                    SpeakerRole.SYSTEM -> "System"
                }
                appendLine("- $roleLabel: ${msg.text}")
            }
            appendLine()
            appendLine("LATEST CALLER UTTERANCE: \"$callerUtterance\"")
            appendLine()
            appendLine("INSTRUCTIONS:")
            appendLine("1. If the caller asks a common question covered in the FAQ list, answer it clearly using that FAQ.")
            appendLine("2. If the caller asks if ${settings.ownerName} is available, state availability accurately (${settings.availabilityStatus.label} — ${settings.availabilityNote}) and offer to take a message or redirect if urgent.")
            appendLine("3. If the caller shares a message, capture a clear summary in capturedMessage and confirm politely.")
            appendLine("4. Only set shouldRedirect = true if Redirect Enabled is true AND (${if (settings.redirectOnUrgent) "the matter is genuinely urgent/emergency" else "never on urgency alone"} OR ${if (settings.redirectOnRequest) "the caller explicitly asks to be transferred/redirected" else "never on transfer request"}). Always specify redirectReason when true.")
            appendLine("5. Keep spokenReply short (1-3 spoken sentences), natural, polite, and without markdown symbols or emojis.")
            appendLine("6. Set shouldEndCall = true only if the caller says goodbye or confirms nothing else is needed after message capture.")
        }

        return invokeStructuredTurn(prompt)
    }

    private fun buildContextBlock(
        settings: AgentSettings,
        faqs: List<FaqItem>,
        callerName: String,
        callerNumber: String
    ): String = buildString {
        appendLine("ROLE: You are ${settings.agentName}, an intelligent personal phone call assistant answering calls on behalf of ${settings.ownerName}.")
        appendLine("OWNER AVAILABILITY: ${settings.availabilityStatus.label} (${settings.availabilityNote.ifBlank { "No additional note" }})")
        appendLine("CALLER INFO: Name='$callerName', Number='$callerNumber'")
        appendLine("LANGUAGE RULE: ${settings.languageMode.instructionHint}")
        appendLine("REDIRECT CONFIG: Enabled=${settings.redirectEnabled}, Target='${settings.redirectTargetName}' (${settings.redirectPhoneNumber.ifBlank { "Not configured" }}), RedirectOnUrgent=${settings.redirectOnUrgent}, RedirectOnRequest=${settings.redirectOnRequest}")
        if (faqs.isNotEmpty()) {
            appendLine("COMMON QUESTIONS (FAQ KNOWLEDGE BASE):")
            faqs.filter { it.isEnabled }.forEachIndexed { idx, faq ->
                appendLine("${idx + 1}. Q: ${faq.question} | A: ${faq.answer}")
            }
        } else {
            appendLine("COMMON QUESTIONS (FAQ KNOWLEDGE BASE): None configured.")
        }
    }

    private suspend fun invokeStructuredTurn(prompt: String): Result<AgentTurnDecision> {
        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content { text(prompt) }
                )
            }
            val jsonText = response.text
            if (jsonText.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Gemini returned an empty response."))
            }
            val obj = JSONObject(jsonText)
            val decision = AgentTurnDecision(
                spokenReply = obj.getString("spokenReply"),
                intent = obj.optString("intent", "GREETING"),
                urgency = runCatching {
                    UrgencyLevel.valueOf(obj.optString("urgency", "LOW"))
                }.getOrDefault(UrgencyLevel.LOW),
                shouldRedirect = obj.optBoolean("shouldRedirect", false),
                redirectReason = obj.optString("redirectReason", "").takeIf {
                    it.isNotBlank() && it != "null"
                },
                capturedCallerName = obj.optString("capturedCallerName", "").takeIf {
                    it.isNotBlank() && it != "null"
                },
                capturedMessage = obj.optString("capturedMessage", "").takeIf {
                    it.isNotBlank() && it != "null"
                },
                shouldEndCall = obj.optBoolean("shouldEndCall", false)
            )
            Result.success(decision)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI inference timed out after 60s", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI inference failed", e)
            Result.failure(e)
        }
    }
}
