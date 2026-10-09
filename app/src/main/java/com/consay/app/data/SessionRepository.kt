package com.consay.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.consay.app.domain.SessionCalculator
import com.consay.app.model.SessionConfig
import com.consay.app.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "con_say_session_prefs")

class SessionRepository(private val context: Context) {

    companion object {
        private val KEY_START_HOUR = intPreferencesKey("start_hour")
        private val KEY_START_MINUTE = intPreferencesKey("start_minute")
        private val KEY_END_HOUR = intPreferencesKey("end_hour")
        private val KEY_END_MINUTE = intPreferencesKey("end_minute")
        private val KEY_RESTRICTED_PACKAGES = stringSetPreferencesKey("restricted_packages")
        private val KEY_START_TIME_MILLIS = longPreferencesKey("start_time_millis")
        private val KEY_END_TIME_MILLIS = longPreferencesKey("end_time_millis")
        private val KEY_STATE = stringPreferencesKey("session_state")
        private val KEY_LAST_INTERVENTION_MILLIS = longPreferencesKey("last_intervention_millis")
    }

    val sessionFlow: Flow<SessionConfig> = context.dataStore.data.map { prefs ->
        val rawStateStr = prefs[KEY_STATE] ?: SessionState.INACTIVE.name
        val rawState = try {
            SessionState.valueOf(rawStateStr)
        } catch (_: Exception) {
            SessionState.INACTIVE
        }

        val config = SessionConfig(
            startHour = prefs[KEY_START_HOUR] ?: 19,
            startMinute = prefs[KEY_START_MINUTE] ?: 0,
            endHour = prefs[KEY_END_HOUR] ?: 1,
            endMinute = prefs[KEY_END_MINUTE] ?: 0,
            restrictedPackages = prefs[KEY_RESTRICTED_PACKAGES] ?: emptySet(),
            startTimeMillis = prefs[KEY_START_TIME_MILLIS] ?: 0L,
            endTimeMillis = prefs[KEY_END_TIME_MILLIS] ?: 0L,
            state = rawState
        )

        // Tự động kiểm tra thời gian thực để cập nhật trạng thái nếu đã hết hạn
        val resolvedState = SessionCalculator.resolveCurrentState(config)
        if (resolvedState != config.state) {
            config.copy(state = resolvedState)
        } else {
            config
        }
    }

    suspend fun getCurrentConfig(): SessionConfig {
        return sessionFlow.first()
    }

    suspend fun saveSetup(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        restrictedPackages: Set<String>
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_START_HOUR] = startHour
            prefs[KEY_START_MINUTE] = startMinute
            prefs[KEY_END_HOUR] = endHour
            prefs[KEY_END_MINUTE] = endMinute
            prefs[KEY_RESTRICTED_PACKAGES] = restrictedPackages
        }
    }

    suspend fun activateSession(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        restrictedPackages: Set<String>
    ): SessionConfig {
        val (startMillis, endMillis) = SessionCalculator.calculateTimestamps(
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute
        )

        val now = System.currentTimeMillis()
        val newState = if (now < startMillis) SessionState.READY else SessionState.ACTIVE

        context.dataStore.edit { prefs ->
            prefs[KEY_START_HOUR] = startHour
            prefs[KEY_START_MINUTE] = startMinute
            prefs[KEY_END_HOUR] = endHour
            prefs[KEY_END_MINUTE] = endMinute
            prefs[KEY_RESTRICTED_PACKAGES] = restrictedPackages
            prefs[KEY_START_TIME_MILLIS] = startMillis
            prefs[KEY_END_TIME_MILLIS] = endMillis
            prefs[KEY_STATE] = newState.name
        }

        return SessionConfig(
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            restrictedPackages = restrictedPackages,
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            state = newState
        )
    }

    suspend fun endSession() {
        context.dataStore.edit { prefs ->
            prefs[KEY_STATE] = SessionState.INACTIVE.name
            prefs[KEY_START_TIME_MILLIS] = 0L
            prefs[KEY_END_TIME_MILLIS] = 0L
        }
    }

    suspend fun updateRestrictedPackages(packages: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RESTRICTED_PACKAGES] = packages
        }
    }

    suspend fun recordInterventionCooldown(cooldownMillis: Long = 60_000L) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_INTERVENTION_MILLIS] = System.currentTimeMillis() + cooldownMillis
        }
    }

    suspend fun isInterventionInCooldown(): Boolean {
        val prefs = context.dataStore.data.first()
        val cooldownUntil = prefs[KEY_LAST_INTERVENTION_MILLIS] ?: 0L
        return System.currentTimeMillis() < cooldownUntil
    }
}
