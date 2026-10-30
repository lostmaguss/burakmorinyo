package com.burakmorinyo.gelirgider.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) { SYSTEM("Sistem"), LIGHT("Açık"), DARK("Koyu") }

data class AppSettings(
    val currency: String = "₺",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    /** Bütçe döneminin başladığı gün (maaş günü) */
    val periodStartDay: Int = 1,
    val appLock: Boolean = false,
    val hideAmounts: Boolean = false,
    val dailyReminder: Boolean = true,
    val reminderHour: Int = 21,
    val budgetAlerts: Boolean = true,
    val paymentReminders: Boolean = true,
    val onboarded: Boolean = false,
    val lastAccountId: Long = 0
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private object K {
        val currency = stringPreferencesKey("currency")
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic")
        val periodStart = intPreferencesKey("period_start")
        val lock = booleanPreferencesKey("lock")
        val hide = booleanPreferencesKey("hide")
        val daily = booleanPreferencesKey("daily_reminder")
        val hour = intPreferencesKey("reminder_hour")
        val budgetAlerts = booleanPreferencesKey("budget_alerts")
        val paymentReminders = booleanPreferencesKey("payment_reminders")
        val onboarded = booleanPreferencesKey("onboarded")
        val lastAccount = longPreferencesKey("last_account")
        val notified = stringSetPreferencesKey("notified")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p -> p.toSettings() }

    suspend fun current(): AppSettings = flow.first()

    private fun Preferences.toSettings() = AppSettings(
        currency = this[K.currency] ?: "₺",
        themeMode = this[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        dynamicColor = this[K.dynamic] ?: false,
        periodStartDay = this[K.periodStart] ?: 1,
        appLock = this[K.lock] ?: false,
        hideAmounts = this[K.hide] ?: false,
        dailyReminder = this[K.daily] ?: true,
        reminderHour = this[K.hour] ?: 21,
        budgetAlerts = this[K.budgetAlerts] ?: true,
        paymentReminders = this[K.paymentReminders] ?: true,
        onboarded = this[K.onboarded] ?: false,
        lastAccountId = this[K.lastAccount] ?: 0
    )

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val s = transform(p.toSettings())
            p[K.currency] = s.currency
            p[K.theme] = s.themeMode.name
            p[K.dynamic] = s.dynamicColor
            p[K.periodStart] = s.periodStartDay
            p[K.lock] = s.appLock
            p[K.hide] = s.hideAmounts
            p[K.daily] = s.dailyReminder
            p[K.hour] = s.reminderHour
            p[K.budgetAlerts] = s.budgetAlerts
            p[K.paymentReminders] = s.paymentReminders
            p[K.onboarded] = s.onboarded
            p[K.lastAccount] = s.lastAccountId
        }
    }

    /** Aynı bildirimi tekrar tekrar göstermemek için gönderilenleri hatırlar. true = ilk kez. */
    suspend fun markNotified(key: String): Boolean {
        var first = false
        context.dataStore.edit { p ->
            val set = p[K.notified] ?: emptySet()
            if (key !in set) {
                first = true
                // Liste şişmesin: son 300 anahtar tutulur
                p[K.notified] = (set.toList().takeLast(299) + key).toSet()
            }
        }
        return first
    }
}
