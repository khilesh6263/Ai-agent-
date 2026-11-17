package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.AgentSettings
import com.example.data.AvailabilityStatus
import com.example.data.LanguageMode
import com.example.data.RedirectMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.agentDataStore: DataStore<Preferences> by preferencesDataStore(name = "agent_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val AGENT_ENABLED = booleanPreferencesKey("agent_enabled")
        val AGENT_NAME = stringPreferencesKey("agent_name")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val AVAILABILITY_STATUS = stringPreferencesKey("availability_status")
        val AVAILABILITY_NOTE = stringPreferencesKey("availability_note")
        val CUSTOM_GREETING = stringPreferencesKey("custom_greeting")
        val AUTO_ANSWER_WHEN_AVAILABLE = booleanPreferencesKey("auto_answer_when_available")
        val AUTO_ANSWER_DELAY = intPreferencesKey("auto_answer_delay")
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val REDIRECT_ENABLED = booleanPreferencesKey("redirect_enabled")
        val REDIRECT_TARGET_NAME = stringPreferencesKey("redirect_target_name")
        val REDIRECT_PHONE_NUMBER = stringPreferencesKey("redirect_phone_number")
        val REDIRECT_MODE = stringPreferencesKey("redirect_mode")
        val REDIRECT_ON_URGENT = booleanPreferencesKey("redirect_on_urgent")
        val REDIRECT_ON_REQUEST = booleanPreferencesKey("redirect_on_request")
        val VIP_NUMBERS_RAW = stringPreferencesKey("vip_numbers_raw")
    }

    val settingsFlow: Flow<AgentSettings> = context.agentDataStore.data.map { prefs ->
        AgentSettings(
            isAgentEnabled = prefs[Keys.AGENT_ENABLED] ?: true,
            agentName = prefs[Keys.AGENT_NAME] ?: "Aria",
            ownerName = prefs[Keys.OWNER_NAME] ?: "Alex",
            availabilityStatus = prefs[Keys.AVAILABILITY_STATUS]?.let {
                runCatching { AvailabilityStatus.valueOf(it) }.getOrNull()
            } ?: AvailabilityStatus.BUSY,
            availabilityNote = prefs[Keys.AVAILABILITY_NOTE] ?: "In a focus session until 4:00 PM",
            customGreeting = prefs[Keys.CUSTOM_GREETING] ?: "",
            autoAnswerWhenAvailable = prefs[Keys.AUTO_ANSWER_WHEN_AVAILABLE] ?: false,
            autoAnswerDelaySeconds = prefs[Keys.AUTO_ANSWER_DELAY] ?: 3,
            languageMode = prefs[Keys.LANGUAGE_MODE]?.let {
                runCatching { LanguageMode.valueOf(it) }.getOrNull()
            } ?: LanguageMode.AUTO_BILINGUAL,
            redirectEnabled = prefs[Keys.REDIRECT_ENABLED] ?: true,
            redirectTargetName = prefs[Keys.REDIRECT_TARGET_NAME] ?: "Team Desk",
            redirectPhoneNumber = prefs[Keys.REDIRECT_PHONE_NUMBER] ?: "",
            redirectMode = prefs[Keys.REDIRECT_MODE]?.let {
                runCatching { RedirectMode.valueOf(it) }.getOrNull()
            } ?: RedirectMode.ALERT_USER,
            redirectOnUrgent = prefs[Keys.REDIRECT_ON_URGENT] ?: true,
            redirectOnRequest = prefs[Keys.REDIRECT_ON_REQUEST] ?: true,
            vipNumbersRaw = prefs[Keys.VIP_NUMBERS_RAW] ?: ""
        )
    }

    suspend fun getSnapshot(): AgentSettings = settingsFlow.first()

    suspend fun updateSettings(transform: (AgentSettings) -> AgentSettings) {
        context.agentDataStore.edit { prefs ->
            val current = AgentSettings(
                isAgentEnabled = prefs[Keys.AGENT_ENABLED] ?: true,
                agentName = prefs[Keys.AGENT_NAME] ?: "Aria",
                ownerName = prefs[Keys.OWNER_NAME] ?: "Alex",
                availabilityStatus = prefs[Keys.AVAILABILITY_STATUS]?.let {
                    runCatching { AvailabilityStatus.valueOf(it) }.getOrNull()
                } ?: AvailabilityStatus.BUSY,
                availabilityNote = prefs[Keys.AVAILABILITY_NOTE] ?: "In a focus session until 4:00 PM",
                customGreeting = prefs[Keys.CUSTOM_GREETING] ?: "",
                autoAnswerWhenAvailable = prefs[Keys.AUTO_ANSWER_WHEN_AVAILABLE] ?: false,
                autoAnswerDelaySeconds = prefs[Keys.AUTO_ANSWER_DELAY] ?: 3,
                languageMode = prefs[Keys.LANGUAGE_MODE]?.let {
                    runCatching { LanguageMode.valueOf(it) }.getOrNull()
                } ?: LanguageMode.AUTO_BILINGUAL,
                redirectEnabled = prefs[Keys.REDIRECT_ENABLED] ?: true,
                redirectTargetName = prefs[Keys.REDIRECT_TARGET_NAME] ?: "Team Desk",
                redirectPhoneNumber = prefs[Keys.REDIRECT_PHONE_NUMBER] ?: "",
                redirectMode = prefs[Keys.REDIRECT_MODE]?.let {
                    runCatching { RedirectMode.valueOf(it) }.getOrNull()
                } ?: RedirectMode.ALERT_USER,
                redirectOnUrgent = prefs[Keys.REDIRECT_ON_URGENT] ?: true,
                redirectOnRequest = prefs[Keys.REDIRECT_ON_REQUEST] ?: true,
                vipNumbersRaw = prefs[Keys.VIP_NUMBERS_RAW] ?: ""
            )
            val updated = transform(current)
            prefs[Keys.AGENT_ENABLED] = updated.isAgentEnabled
            prefs[Keys.AGENT_NAME] = updated.agentName
            prefs[Keys.OWNER_NAME] = updated.ownerName
            prefs[Keys.AVAILABILITY_STATUS] = updated.availabilityStatus.name
            prefs[Keys.AVAILABILITY_NOTE] = updated.availabilityNote
            prefs[Keys.CUSTOM_GREETING] = updated.customGreeting
            prefs[Keys.AUTO_ANSWER_WHEN_AVAILABLE] = updated.autoAnswerWhenAvailable
            prefs[Keys.AUTO_ANSWER_DELAY] = updated.autoAnswerDelaySeconds
            prefs[Keys.LANGUAGE_MODE] = updated.languageMode.name
            prefs[Keys.REDIRECT_ENABLED] = updated.redirectEnabled
            prefs[Keys.REDIRECT_TARGET_NAME] = updated.redirectTargetName
            prefs[Keys.REDIRECT_PHONE_NUMBER] = updated.redirectPhoneNumber
            prefs[Keys.REDIRECT_MODE] = updated.redirectMode.name
            prefs[Keys.REDIRECT_ON_URGENT] = updated.redirectOnUrgent
            prefs[Keys.REDIRECT_ON_REQUEST] = updated.redirectOnRequest
            prefs[Keys.VIP_NUMBERS_RAW] = updated.vipNumbersRaw
        }
    }
}
