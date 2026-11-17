package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.FaqEntity
import com.example.data.local.SettingsDataStore
import com.example.telecom.CallSessionController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class CallAgentRepository(context: Context) {

    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val settingsStore = SettingsDataStore(appContext)
    private val sessionController = CallSessionController.getInstance(appContext)

    val settingsFlow: Flow<AgentSettings> = settingsStore.settingsFlow

    val faqsFlow: Flow<List<FaqItem>> = database.faqDao().observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val callRecordsFlow: Flow<List<CallRecord>> = database.callRecordDao().observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val liveCallState: StateFlow<LiveCallState> = sessionController.liveState

    suspend fun updateSettings(transform: (AgentSettings) -> AgentSettings) {
        settingsStore.updateSettings(transform)
    }

    suspend fun saveFaq(item: FaqItem) {
        val entity = FaqEntity.fromDomain(item)
        if (item.id == 0L) {
            database.faqDao().insert(entity)
        } else {
            database.faqDao().update(entity)
        }
    }

    suspend fun toggleFaqEnabled(item: FaqItem) {
        database.faqDao().update(FaqEntity.fromDomain(item.copy(isEnabled = !item.isEnabled)))
    }

    suspend fun deleteFaq(id: Long) {
        database.faqDao().deleteById(id)
    }

    suspend fun setCallResolved(id: Long, resolved: Boolean) {
        database.callRecordDao().setResolved(id, resolved)
    }

    suspend fun deleteCallRecord(id: Long) {
        database.callRecordDao().deleteById(id)
    }

    suspend fun clearAllCallRecords() {
        database.callRecordDao().clearAll()
    }

    fun startSimulatedCall(callerName: String, callerNumber: String) {
        sessionController.startIncomingAiSession(
            callerName = callerName,
            callerNumber = callerNumber,
            isSimulated = true,
            autoVoiceListen = false
        )
    }

    fun sendSimulatedCallerMessage(message: String, speakAloud: Boolean = true) {
        sessionController.submitCallerInput(message, speakReplyAloud = speakAloud)
    }

    fun triggerMicListening() {
        sessionController.triggerVoiceCaptureOnce()
    }

    fun performRedirect() {
        sessionController.performManualRedirect()
    }

    fun endLiveCall() {
        sessionController.endCallSession()
    }
}
