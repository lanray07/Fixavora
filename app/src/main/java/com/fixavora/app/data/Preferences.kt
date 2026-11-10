package com.fixavora.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.fixavoraSettings by preferencesDataStore("fixavora_settings")
data class Preferences(val language: String = "", val currency: String = "GBP", val dark: Boolean = false, val contrast: Boolean = false, val spoken: Boolean = false, val reminders: Boolean = false, val quietStart: Int = 22, val quietEnd: Int = 8, val onboarding: Boolean = false, val units: String = "metric", val role: String = "homeowner")
class Settings(private val context: Context) {
    val flow = context.fixavoraSettings.data.map { p -> Preferences(
        p[stringPreferencesKey("language")] ?: "", p[stringPreferencesKey("currency")] ?: "GBP",
        p[booleanPreferencesKey("dark")] ?: false, p[booleanPreferencesKey("contrast")] ?: false,
        p[booleanPreferencesKey("spoken")] ?: false, p[booleanPreferencesKey("reminders")] ?: false,
        p[intPreferencesKey("quietStart")] ?: 22, p[intPreferencesKey("quietEnd")] ?: 8,
        p[booleanPreferencesKey("onboarding")] ?: false, p[stringPreferencesKey("units")] ?: "metric", p[stringPreferencesKey("role")] ?: "homeowner"
    ) }
    suspend fun text(key: String, value: String) { context.fixavoraSettings.edit { it[stringPreferencesKey(key)] = value } }
    suspend fun flag(key: String, value: Boolean) { context.fixavoraSettings.edit { it[booleanPreferencesKey(key)] = value } }
    suspend fun number(key: String, value: Int) { context.fixavoraSettings.edit { it[intPreferencesKey(key)] = value } }
    suspend fun clear() { context.fixavoraSettings.edit { it.clear() } }
}
