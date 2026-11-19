package com.scanpilot.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** App settings — the API key is stored only on the user's phone and never in the code. */
data class AiSettings(
    val baseUrl: String = "https://coarena.ai/api/v1",
    val model: String = "claude-fable-5-2",
    val apiKey: String = "",
    val aiEnabled: Boolean = false
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val API_KEY = stringPreferencesKey("api_key")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
    }

    val settings: Flow<AiSettings> = context.dataStore.data.map { prefs ->
        AiSettings(
            baseUrl = prefs[Keys.BASE_URL] ?: "https://coarena.ai/api/v1",
            model = prefs[Keys.MODEL] ?: "claude-fable-5-2",
            apiKey = prefs[Keys.API_KEY] ?: "",
            aiEnabled = prefs[Keys.AI_ENABLED] ?: false
        )
    }

    suspend fun save(settings: AiSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = settings.baseUrl.trim().trimEnd('/')
            prefs[Keys.MODEL] = settings.model.trim()
            prefs[Keys.API_KEY] = settings.apiKey.trim()
            prefs[Keys.AI_ENABLED] = settings.aiEnabled
        }
    }

    // ---- Onboarding, usage counts and Pro status (all local, no user account) ----

    private object ExtraKeys {
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        val SUCCESSFUL_USES = androidx.datastore.preferences.core.intPreferencesKey("successful_uses")
        val RATE_PROMPT_SHOWN = booleanPreferencesKey("rate_prompt_shown")
        val SCANS_TODAY = androidx.datastore.preferences.core.intPreferencesKey("scans_today")
        val LAST_SCAN_DATE = stringPreferencesKey("last_scan_date")
        val IS_PRO = booleanPreferencesKey("is_pro")
    }

    val onboardingSeen: Flow<Boolean> = context.dataStore.data.map { it[ExtraKeys.ONBOARDING_SEEN] ?: false }

    suspend fun setOnboardingSeen() {
        context.dataStore.edit { it[ExtraKeys.ONBOARDING_SEEN] = true }
    }

    val successfulUses: Flow<Int> = context.dataStore.data.map { it[ExtraKeys.SUCCESSFUL_USES] ?: 0 }

    suspend fun recordSuccessfulUse() {
        context.dataStore.edit { it[ExtraKeys.SUCCESSFUL_USES] = (it[ExtraKeys.SUCCESSFUL_USES] ?: 0) + 1 }
    }

    val ratePromptShown: Flow<Boolean> = context.dataStore.data.map { it[ExtraKeys.RATE_PROMPT_SHOWN] ?: false }

    suspend fun setRatePromptShown() {
        context.dataStore.edit { it[ExtraKeys.RATE_PROMPT_SHOWN] = true }
    }

    /** Number of scans today (by the Gregorian calendar date, for the free-version cap). */
    val scansToday: Flow<Int> = context.dataStore.data.map { prefs ->
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        if ((prefs[ExtraKeys.LAST_SCAN_DATE] ?: "") == today) prefs[ExtraKeys.SCANS_TODAY] ?: 0 else 0
    }

    suspend fun recordScan() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        context.dataStore.edit { prefs ->
            val count = if ((prefs[ExtraKeys.LAST_SCAN_DATE] ?: "") == today) {
                (prefs[ExtraKeys.SCANS_TODAY] ?: 0) + 1
            } else 1
            prefs[ExtraKeys.SCANS_TODAY] = count
            prefs[ExtraKeys.LAST_SCAN_DATE] = today
        }
    }

    /** Pro status — currently only a local flag (stub); real purchases will connect via Google Play Billing. */
    val isPro: Flow<Boolean> = context.dataStore.data.map { it[ExtraKeys.IS_PRO] ?: false }

    suspend fun setPro(value: Boolean) {
        context.dataStore.edit { it[ExtraKeys.IS_PRO] = value }
    }
}
