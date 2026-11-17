package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AgentSettings
import com.example.data.AvailabilityStatus
import com.example.data.CallOutcome
import com.example.data.CallRecord
import com.example.data.SpeakerRole
import com.example.data.TranscriptMessage
import com.example.data.UrgencyLevel
import com.example.data.local.CallRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CallAgent", appName)
    }

    @Test
    fun `agent auto-answer respects availability and vip bypass`() {
        val busySettings = AgentSettings(
            isAgentEnabled = true,
            availabilityStatus = AvailabilityStatus.BUSY,
            autoAnswerWhenAvailable = false,
            vipNumbersRaw = "+91 98111 00998, 9999988888"
        )
        assertTrue(busySettings.shouldAutoAnswerCalls)
        assertTrue(busySettings.isVipNumber("9811100998"))
        assertFalse(busySettings.isVipNumber("9820111223"))

        val availableSettings = busySettings.copy(availabilityStatus = AvailabilityStatus.AVAILABLE)
        assertFalse(availableSettings.shouldAutoAnswerCalls)
    }

    @Test
    fun `call record entity round-trips transcript json`() {
        val original = CallRecord(
            id = 42L,
            callerName = "Priya Sharma",
            callerNumber = "+91 99104 55678",
            startedAtMillis = 1700000000000L,
            durationSeconds = 38,
            availabilityAtCall = AvailabilityStatus.IN_MEETING,
            outcome = CallOutcome.MESSAGE_TAKEN,
            urgency = UrgencyLevel.HIGH,
            capturedMessage = "Call back after 5 PM about invoice",
            redirectTarget = null,
            isSimulated = true,
            isResolved = false,
            transcript = listOf(
                TranscriptMessage("1", SpeakerRole.AGENT, "Hi, Alex is in a meeting.", 1700000001000L, "GREETING"),
                TranscriptMessage("2", SpeakerRole.CALLER, "Please ask him to call back after 5 PM.", 1700000005000L)
            )
        )

        val restored = CallRecordEntity.fromDomain(original).toDomain()
        assertEquals(original.callerName, restored.callerName)
        assertEquals(original.outcome, restored.outcome)
        assertEquals(original.urgency, restored.urgency)
        assertEquals(2, restored.transcript.size)
        assertEquals("GREETING", restored.transcript.first().intentTag)
    }
}
