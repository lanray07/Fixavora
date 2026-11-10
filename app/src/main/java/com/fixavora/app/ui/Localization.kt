package com.fixavora.app.ui

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

val languages = linkedMapOf("" to "System", "en-GB" to "English (UK)", "en-US" to "English (US)", "en-AU" to "English (Australia)", "es-ES" to "Español", "es-419" to "Español (Latinoamérica)", "fr-FR" to "Français", "fr-CA" to "Français (Canada)", "de-DE" to "Deutsch", "it-IT" to "Italiano", "pt-BR" to "Português (Brasil)", "pt-PT" to "Português (Portugal)", "nl-NL" to "Nederlands", "pl-PL" to "Polski", "sv-SE" to "Svenska", "ja-JP" to "日本語", "ko-KR" to "한국어", "zh-Hans" to "简体中文", "zh-Hant" to "繁體中文", "hi-IN" to "हिन्दी", "ar" to "العربية", "tr-TR" to "Türkçe", "id-ID" to "Bahasa Indonesia")
fun Context.localized(tag: String): Context {
    val config = Configuration(resources.configuration)
    if (tag.isNotEmpty()) config.setLocale(Locale.forLanguageTag(tag))
    // Keep the Activity in the ContextWrapper chain. Compose activity-result launchers
    // must still resolve its registry when using a per-app language configuration.
    return android.view.ContextThemeWrapper(this, 0).apply { applyOverrideConfiguration(config) }
}
fun Context.t(key: String): String {
    val name=if(key.lowercase()=="in_progress") "fixavora_in_progress" else key.lowercase()
    val id = resources.getIdentifier(name, "string", packageName)
    return getString(if (id == 0) RStringFallback else id)
}
private val RStringFallback = com.fixavora.app.R.string.general

val currencyChoices: Map<String,String> = java.util.Currency.getAvailableCurrencies()
    .filter { it.defaultFractionDigits >= 0 }.map { it.currencyCode }.sorted().associateWith { it }
