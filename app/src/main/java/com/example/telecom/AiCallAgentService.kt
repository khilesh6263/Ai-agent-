package com.example.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class AiCallAgentService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                CallSessionController.getInstance(applicationContext).endCallSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_CALL -> {
                val callerName = intent.getStringExtra(EXTRA_CALLER_NAME).orEmpty()
                val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER).orEmpty()
                val isSimulated = intent.getBooleanExtra(EXTRA_IS_SIMULATED, false)

                createChannelIfNeeded()
                val notification = buildNotification(
                    callerLabel = callerName.ifBlank { callerNumber.ifBlank { "Incoming Caller" } }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                CallSessionController.getInstance(applicationContext).startIncomingAiSession(
                    callerName = callerName,
                    callerNumber = callerNumber,
                    isSimulated = isSimulated,
                    autoVoiceListen = !isSimulated
                )
            }
        }
        return START_NOT_STICKY
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(callerLabel: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AiCallAgentService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_active_call_title))
            .setContentText("Handling call with $callerLabel")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPending)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_end_call),
                stopPending
            )
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "ai_call_agent_channel"
        private const val NOTIFICATION_ID = 4042
        const val ACTION_START_CALL = "com.example.telecom.ACTION_START_CALL"
        const val ACTION_STOP = "com.example.telecom.ACTION_STOP"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CALLER_NUMBER = "extra_caller_number"
        const val EXTRA_IS_SIMULATED = "extra_is_simulated"

        fun start(
            context: Context,
            callerName: String,
            callerNumber: String,
            isSimulated: Boolean
        ) {
            val intent = Intent(context, AiCallAgentService::class.java).apply {
                action = ACTION_START_CALL
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_CALLER_NUMBER, callerNumber)
                putExtra(EXTRA_IS_SIMULATED, isSimulated)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, AiCallAgentService::class.java))
            }
        }
    }
}
