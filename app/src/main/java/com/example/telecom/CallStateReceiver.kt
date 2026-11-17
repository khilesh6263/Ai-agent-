package com.example.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "CallStateReceiver"

class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        @Suppress("DEPRECATION")
        val numberExtra = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
        if (numberExtra.isNotBlank()) {
            RecentIncomingCallCache.update(RecentIncomingCallCache.lastCallerName, numberExtra)
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        receiverScope.launch {
            try {
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        handleRinging(appContext, numberExtra)
                    }
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        if (pendingAutoAnswerSession) {
                            pendingAutoAnswerSession = false
                            val number = RecentIncomingCallCache.lastCallerNumber.ifBlank { numberExtra }
                            val name = RecentIncomingCallCache.lastCallerName.ifBlank { "Incoming Caller" }
                            AiCallAgentService.start(
                                context = appContext,
                                callerName = name,
                                callerNumber = number,
                                isSimulated = false
                            )
                        }
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        autoAnswerJob?.cancel()
                        pendingAutoAnswerSession = false
                        if (CallSessionController.getInstance(appContext).liveState.value.let {
                                it.isActive && !it.isSimulated
                            }
                        ) {
                            CallSessionController.getInstance(appContext).endCallSession()
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleRinging(appContext: Context, numberExtra: String) {
        autoAnswerJob?.cancel()
        autoAnswerJob = receiverScope.launch {
            val settings = SettingsDataStore(appContext).getSnapshot()
            val callerNumber = numberExtra.ifBlank { RecentIncomingCallCache.lastCallerNumber }

            if (!settings.shouldAutoAnswerCalls) {
                Log.d(TAG, "AI auto-answer paused or user is Available; letting phone ring.")
                return@launch
            }
            if (settings.isVipNumber(callerNumber)) {
                Log.d(TAG, "VIP caller $callerNumber detected; bypassing AI auto-answer.")
                return@launch
            }

            val hasAnswerPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.ANSWER_PHONE_CALLS
                ) == PackageManager.PERMISSION_GRANTED

            if (!hasAnswerPerm) {
                Log.w(TAG, "Missing ANSWER_PHONE_CALLS permission; cannot auto-answer.")
                return@launch
            }

            val delayMs = (settings.autoAnswerDelaySeconds.coerceIn(0, 15) * 1000L)
            if (delayMs > 0) {
                delay(delayMs)
            }

            val telecomManager = appContext.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pendingAutoAnswerSession = true
                runCatching {
                    @Suppress("DEPRECATION")
                    telecomManager.acceptRingingCall()
                    Log.d(TAG, "TelecomManager.acceptRingingCall() invoked successfully.")
                }.onFailure { err ->
                    pendingAutoAnswerSession = false
                    Log.e(TAG, "Failed to accept ringing call", err)
                }
            }
        }
    }

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var autoAnswerJob: Job? = null

        @Volatile
        private var pendingAutoAnswerSession = false
    }
}
