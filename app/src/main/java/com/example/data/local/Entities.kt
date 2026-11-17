package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.AvailabilityStatus
import com.example.data.CallOutcome
import com.example.data.CallRecord
import com.example.data.FaqItem
import com.example.data.SpeakerRole
import com.example.data.TranscriptMessage
import com.example.data.UrgencyLevel
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "faqs")
data class FaqEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val answer: String,
    val category: String,
    val isEnabled: Boolean
) {
    fun toDomain() = FaqItem(
        id = id,
        question = question,
        answer = answer,
        category = category,
        isEnabled = isEnabled
    )

    companion object {
        fun fromDomain(item: FaqItem) = FaqEntity(
            id = item.id,
            question = item.question,
            answer = item.answer,
            category = item.category,
            isEnabled = item.isEnabled
        )
    }
}

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callerName: String,
    val callerNumber: String,
    val startedAtMillis: Long,
    val durationSeconds: Int,
    val availabilityAtCall: String,
    val outcome: String,
    val urgency: String,
    val capturedMessage: String,
    val redirectTarget: String?,
    val isSimulated: Boolean,
    val isResolved: Boolean,
    val transcriptJson: String
) {
    fun toDomain(): CallRecord {
        val parsedTranscript = mutableListOf<TranscriptMessage>()
        try {
            val array = JSONArray(transcriptJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                parsedTranscript.add(
                    TranscriptMessage(
                        id = obj.optString("id", "$i"),
                        speaker = runCatching {
                            SpeakerRole.valueOf(obj.optString("speaker", "SYSTEM"))
                        }.getOrDefault(SpeakerRole.SYSTEM),
                        text = obj.optString("text", ""),
                        timestampMillis = obj.optLong("timestampMillis", startedAtMillis),
                        intentTag = obj.optString("intentTag", "").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (_: Exception) {
            // Keep empty list if JSON malformed
        }

        return CallRecord(
            id = id,
            callerName = callerName,
            callerNumber = callerNumber,
            startedAtMillis = startedAtMillis,
            durationSeconds = durationSeconds,
            availabilityAtCall = runCatching {
                AvailabilityStatus.valueOf(availabilityAtCall)
            }.getOrDefault(AvailabilityStatus.BUSY),
            outcome = runCatching {
                CallOutcome.valueOf(outcome)
            }.getOrDefault(CallOutcome.MISSED_OR_ENDED),
            urgency = runCatching {
                UrgencyLevel.valueOf(urgency)
            }.getOrDefault(UrgencyLevel.LOW),
            capturedMessage = capturedMessage,
            redirectTarget = redirectTarget,
            isSimulated = isSimulated,
            isResolved = isResolved,
            transcript = parsedTranscript
        )
    }

    companion object {
        fun fromDomain(record: CallRecord): CallRecordEntity {
            val array = JSONArray()
            record.transcript.forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("speaker", msg.speaker.name)
                    put("text", msg.text)
                    put("timestampMillis", msg.timestampMillis)
                    if (msg.intentTag != null) put("intentTag", msg.intentTag)
                }
                array.put(obj)
            }
            return CallRecordEntity(
                id = record.id,
                callerName = record.callerName,
                callerNumber = record.callerNumber,
                startedAtMillis = record.startedAtMillis,
                durationSeconds = record.durationSeconds,
                availabilityAtCall = record.availabilityAtCall.name,
                outcome = record.outcome.name,
                urgency = record.urgency.name,
                capturedMessage = record.capturedMessage,
                redirectTarget = record.redirectTarget,
                isSimulated = record.isSimulated,
                isResolved = record.isResolved,
                transcriptJson = array.toString()
            )
        }
    }
}
