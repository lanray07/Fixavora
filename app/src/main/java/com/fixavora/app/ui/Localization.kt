package com.fixavora.app.ui

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

val languages = linkedMapOf("" to "System", "en-GB" to "English (UK)", "en-US" to "English (US)", "en-AU" to "English (Australia)", "es-ES" to "Español", "es-419" to "Español (Latinoamérica)", "fr-FR" to "Français", "fr-CA" to "Français (Canada)", "de-DE" to "Deutsch", "it-IT" to "Italiano", "pt-BR" to "Português (Brasil)", "pt-PT" to "Português (Portugal)", "nl-NL" to "Nederlands", "pl-PL" to "Polski", "sv-SE" to "Svenska", "ja-JP" to "日本語", "hi-IN" to "हिन्दी", "ar" to "العربية", "tr-TR" to "Türkçe", "id-ID" to "Bahasa Indonesia")
fun Context.localized(tag: String): Context {
    val config = Configuration(resources.configuration)
    if (tag.isNotEmpty()) config.setLocale(Locale.forLanguageTag(tag))
    return createConfigurationContext(config)
}
fun Context.t(key: String): String {
    val id = resources.getIdentifier(key.lowercase(), "string", packageName)
    return getString(if (id == 0) RStringFallback else id)
}
private val RStringFallback = com.fixavora.app.R.string.general
