package com.fixavora.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fixavora.app.data.*
import com.fixavora.app.domain.*
import com.fixavora.app.reports.Exports
import com.fixavora.app.voice.VoiceController
import java.time.LocalDate
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable fun AssistantPanel(vm: HomeViewModel, property: HomeRecord?, records: List<HomeRecord>, locale: Locale, prefs: Preferences, edit: (HomeRecord) -> Unit) {
    val c = LocalContext.current
    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var proposal by remember { mutableStateOf<ProposedAction?>(null) }
    var answer by remember { mutableStateOf("") }
    var completion by remember { mutableStateOf<HomeRecord?>(null) }
    val dictionary = remember(locale) {
        runCatching {
            val all = c.assets.open("voice-commands.json").bufferedReader().use { Gson().fromJson<Map<String, Map<String, List<String>>>>(it, object : TypeToken<Map<String, Map<String, List<String>>>>() {}.type) }
            all[locale.toLanguageTag()] ?: all.entries.firstOrNull { it.key.substringBefore('-') == locale.language }?.value ?: all.getValue("en-GB")
        }.getOrDefault(emptyMap())
    }
    val voice = remember { VoiceController(c) { active, transcript, error -> listening = active; transcript?.let { input = it }; error?.let { vm.message.value = it } } }
    DisposableEffect(voice) { onDispose { voice.close() } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) voice.start(locale) else vm.message.value = "microphone_denied" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EmptyState(c.t("a_helping_hand"), c.t("local_assistant_intro"), Icons.Outlined.AutoAwesome)
        Text(c.t("voice_privacy"), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(input, { input = it; proposal = null }, Modifier.fillMaxWidth(), label = { Text(c.t("type_or_speak")) }, minLines = 3)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({
                if (!voice.available) vm.message.value = "voice_unavailable"
                else if (listening) voice.stop()
                else if (c.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) voice.start(locale)
                else permission.launch(Manifest.permission.RECORD_AUDIO)
            }) { Icon(if (listening) Icons.Outlined.StopCircle else Icons.Outlined.MicNone, null); Text(c.t(if (listening) "stop_listening" else "push_to_talk")) }
            Button({
                val today = LocalDate.now()
                answer = ""
                when (VoiceIntents.parse(input, dictionary)) {
                    VoiceIntent.DUE_WEEK -> answer = records.filter { it.due != null && it.due >= today.toString() && it.due <= today.plusDays(7).toString() && !it.done }.joinToString("\n") { it.title }.ifEmpty { c.t("nothing_due") }
                    VoiceIntent.OVERDUE -> answer = records.filter { it.kind == "TASK" && it.due != null && it.due < today.toString() && !it.done }.joinToString("\n") { it.title }.ifEmpty { c.t("nothing_due") }
                    VoiceIntent.EXPENSES -> answer = Exports.summary(c, records.filter { it.kind == "EXPENSE" }, locale, c.t("all_dates")).joinToString("\n")
                    VoiceIntent.WARRANTIES -> answer = records.filter { it.kind in setOf("WARRANTY", "APPLIANCE") && it.due != null }.joinToString("\n") { it.title + " • " + formattedDate(it.due!!, locale) }.ifEmpty { c.t("no_records") }
                    VoiceIntent.COMPLETE -> {
                        val matches = records.filter { it.kind == "TASK" && !it.done && input.contains(it.title, ignoreCase = true) }
                        if (matches.size == 1) completion = matches.single() else answer = c.t("choose_task_to_complete")
                    }
                    VoiceIntent.APPOINTMENT -> if (property != null) edit(newRecord(Kind.APPOINTMENT, property.id, prefs).copy(title = input)) else vm.message.value = "create_property_first"
                    VoiceIntent.CREATE -> proposal = LocalAssistant.propose(input, today)
                }
                if (answer.isNotEmpty() && prefs.spoken) voice.speak(answer, locale) { vm.message.value = "tts_unavailable" }
            }, enabled = input.isNotBlank()) { Text(c.t("prepare_task")) }
        }
        if (listening) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(c.t("listening")) }
        if (!voice.available) Text(c.t("voice_unavailable"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        proposal?.let { p ->
            ElevatedCard { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Heading(c.t("review_proposal"), p.title)
                Text(c.t(p.category) + " • " + (p.due?.let { formattedDate(it.toString(), locale) } ?: c.t("no_date")) + " • " + c.t(p.repeat.name.lowercase()))
                if (p.hazardous) Text(c.t("safety_guardrail"), color = MaterialTheme.colorScheme.error)
                val duplicates = LocalAssistant.duplicates(p.title, records.filter { it.kind == "TASK" }.map { it.title })
                if (duplicates.isNotEmpty()) Text(c.t("possible_duplicate") + ": " + duplicates.joinToString())
                Button({ if (property == null) vm.message.value = "create_property_first" else edit(newRecord(Kind.TASK, property.id, prefs).copy(title = p.title, category = p.category, due = p.due?.toString(), anchorDay = p.due?.dayOfMonth ?: 1, repeat = p.repeat.name)); proposal = null }) { Text(c.t("review_and_save")) }
            } }
        }
        Heading(c.t("from_your_records"), c.t("assistant_reads_local"))
        listOf("read_overdue", "week_summary", "spending_summary", "warranty_summary", "contractor_questions", "maintenance_template").forEach { key ->
            OutlinedButton({
                answer = when (key) {
                    "read_overdue" -> records.filter { it.kind == "TASK" && !it.done && it.due != null && it.due < LocalDate.now().toString() }.joinToString("\n") { it.title }.ifEmpty { c.t("nothing_due") }
                    "week_summary" -> records.filter { it.due != null && it.due >= LocalDate.now().toString() && it.due <= LocalDate.now().plusDays(7).toString() && !it.done }.joinToString("\n") { it.title + " • " + formattedDate(it.due!!, locale) }.ifEmpty { c.t("nothing_due") }
                    "warranty_summary" -> records.filter { it.kind in setOf("WARRANTY", "APPLIANCE") && it.due != null }.joinToString("\n") { it.title + " • " + formattedDate(it.due!!, locale) }.ifEmpty { c.t("no_records") }
                    "spending_summary" -> Exports.summary(c, records.filter { it.kind == "EXPENSE" }, locale, c.t("all_dates")).joinToString("\n")
                    "contractor_questions" -> c.t("contractor_questions_text")
                    else -> c.t("maintenance_template_text")
                }
                if (prefs.spoken) voice.speak(answer, locale) { vm.message.value = "tts_unavailable" }
            }, Modifier.fillMaxWidth()) { Text(c.t(key)) }
        }
        if (answer.isNotEmpty()) { Text(answer); TextButton({ voice.speak(answer, locale) { vm.message.value = "tts_unavailable" } }) { Text(c.t("read_aloud")) } }
        Text(c.t("remote_ai_unavailable"), style = MaterialTheme.typography.bodySmall)
    }
    completion?.let { task -> AlertDialog(onDismissRequest = { completion = null }, title = { Text(c.t("mark_complete")) }, text = { Text(task.title) }, confirmButton = { TextButton({ completion = null; vm.action { vm.repo.complete(task.id) } }) { Text(c.t("mark_complete")) } }, dismissButton = { TextButton({ completion = null }) { Text(c.t("cancel")) } }) }
}
