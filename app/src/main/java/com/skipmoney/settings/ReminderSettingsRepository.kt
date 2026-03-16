package com.skipmoney.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderSettingsDataStore by preferencesDataStore(name = "reminder_settings")

data class ReminderSettings(
    val notificationsEnabled: Boolean = true,
    val notificationHour: Int = 20,
    val notificationMinute: Int = 0,
    val onboardingCompleted: Boolean = false,
)

class ReminderSettingsRepository(
    private val context: Context,
) {

    val settings: Flow<ReminderSettings> =
        context.reminderSettingsDataStore.data.map { preferences ->
            ReminderSettings(
                notificationsEnabled = preferences[NOTIFICATIONS_ENABLED_KEY] ?: true,
                notificationHour = preferences[NOTIFICATION_HOUR_KEY] ?: 20,
                notificationMinute = preferences[NOTIFICATION_MINUTE_KEY] ?: 0,
                onboardingCompleted = preferences[ONBOARDING_COMPLETED_KEY] ?: false,
            )
        }

    suspend fun getCurrentSettings(): ReminderSettings = settings.first()

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.reminderSettingsDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateNotificationTime(hour: Int, minute: Int) {
        context.reminderSettingsDataStore.edit { preferences ->
            preferences[NOTIFICATION_HOUR_KEY] = hour
            preferences[NOTIFICATION_MINUTE_KEY] = minute
        }
    }

    suspend fun completeOnboarding() {
        context.reminderSettingsDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
    }

    companion object {
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val NOTIFICATION_HOUR_KEY = intPreferencesKey("notification_hour")
        private val NOTIFICATION_MINUTE_KEY = intPreferencesKey("notification_minute")
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    }
}
