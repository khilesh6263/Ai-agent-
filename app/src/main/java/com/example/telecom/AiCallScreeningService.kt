package com.example.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "AiCallScreeningService"

class AiCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        val incomingNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        val callerDisplayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            callDetails.callerDisplayName.orEmpty()
        } else {
            ""
        }

        if (isIncoming && incomingNumber.isNotBlank()) {
            RecentIncomingCallCache.update(callerDisplayName, incomingNumber)
        }

        serviceScope.launch {
            val settings = runCatching {
                SettingsDataStore(applicationContext).getSnapshot()
            }.getOrNull()

            val silenceRing = settings != null &&
                settings.shouldAutoAnswerCalls &&
                !settings.isVipNumber(incomingNumber)

            Log.d(TAG, "Screening incoming call from $incomingNumber; autoAnswer=${settings?.shouldAutoAnswerCalls}")

            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(silenceRing)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

            respondToCall(callDetails, response)
        }
    }
}

object RecentIncomingCallCache {
    @Volatile
    var lastCallerName: String = ""
        private set

    @Volatile
    var lastCallerNumber: String = ""
        private set

    fun update(name: String, number: String) {
        lastCallerName = name
        lastCallerNumber = number
    }
}
