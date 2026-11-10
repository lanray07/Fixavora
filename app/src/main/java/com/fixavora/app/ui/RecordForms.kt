@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.fixavora.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fixavora.app.data.*
import com.fixavora.app.domain.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

val categories = listOf("general", "heating", "plumbing", "electrical", "roofing", "exterior", "garden", "cleaning", "ventilation", "safety", "appliances")
data class ExtraField(val key: String, val type: String = "text", val choices: List<String> = emptyList())
val extraFields = mapOf(
    Kind.PROPERTY to listOf(ExtraField("property_type", choices = listOf("house", "flat", "other")), ExtraField("tenure", choices = listOf("owned", "rented", "managed")), ExtraField("address")),
    Kind.ROOM to listOf(ExtraField("room_type", choices = listOf("kitchen", "bathroom", "living_room", "bedroom", "garage", "garden", "utility_room", "loft", "basement", "custom"))),
    Kind.TASK to emptyList(),
    Kind.REPAIR to listOf(ExtraField("description"), ExtraField("reported_date", "date"), ExtraField("completion_date", "date"), ExtraField("estimated_cost", "money"), ExtraField("actual_cost", "money")),
    Kind.APPLIANCE to listOf(ExtraField("manufacturer"), ExtraField("model"), ExtraField("serial_number"), ExtraField("purchase_date", "date"), ExtraField("installation_date", "date"), ExtraField("purchase_price", "money"), ExtraField("service_history")),
    Kind.WARRANTY to listOf(ExtraField("start_date", "date"), ExtraField("provider"), ExtraField("coverage_notes")),
    Kind.CONTRACTOR to listOf(ExtraField("trade"), ExtraField("business_name"), ExtraField("phone", "phone"), ExtraField("email", "email"), ExtraField("website", "url")),
    Kind.EXPENSE to emptyList(),
    Kind.APPOINTMENT to listOf(ExtraField("time", "time"), ExtraField("duration_minutes", "number"))
)
fun fieldsOf(r: HomeRecord): Map<String, String> = runCatching { Gson().fromJson<Map<String, String>>(r.fields, object : TypeToken<Map<String, String>>() {}.type) }.getOrDefault(emptyMap())

@Composable fun RecordEditor(initial: HomeRecord, all: List<HomeRecord>, dismiss: () -> Unit, save: (HomeRecord) -> Unit) {
    val c = LocalContext.current
    val inputLocale = LocalConfiguration.current.locales[0]
    val kind = Kind.valueOf(initial.kind)
    var title by remember { mutableStateOf(initial.title) }
    var notes by remember { mutableStateOf(initial.notes) }
    var property by remember { mutableStateOf(initial.propertyId.orEmpty()) }
    var room by remember { mutableStateOf(initial.roomId.orEmpty()) }
    var contractor by remember { mutableStateOf(initial.contractorId.orEmpty()) }
    var related by remember { mutableStateOf(initial.relatedId.orEmpty()) }
    var due by remember { mutableStateOf(initial.due.orEmpty()) }
    var date by remember { mutableStateOf(initial.date) }
    var category by remember { mutableStateOf(initial.category) }
    var priority by remember { mutableStateOf(initial.priority) }
    var status by remember { mutableStateOf(initial.status) }
    var repeat by remember { mutableStateOf(initial.repeat) }
    var interval by remember { mutableStateOf(initial.interval.toString()) }
    var paused by remember { mutableStateOf(initial.paused) }
    var currency by remember { mutableStateOf(initial.currency) }
    var amount by remember { mutableStateOf(Money.decimal(initial.amountMinor, initial.currency).toPlainString()) }
    val extras = remember { mutableStateMapOf<String, String>().apply { putAll(fieldsOf(initial)) } }
    var error by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = dismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxWidth(.94f).widthIn(max = 720.dp).fillMaxHeight(.94f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(24.dp)) {
                Heading(c.t(if (initial.title.isBlank()) "add_record" else "edit_record"), c.t(initial.kind.lowercase()))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditField("title", title, { title = it }, required = true)
                    if (kind != Kind.PROPERTY) {
                        Text(c.t("property")); Choice(property, all.filter { it.kind == "PROPERTY" && (!it.archived || it.id == initial.propertyId) }.associate { it.id to it.title }, { property = it; room = ""; contractor = ""; related = "" })
                        if (kind != Kind.ROOM) {
                            Text(c.t("room")); Choice(room, mapOf("" to c.t("none")) + all.filter { it.kind == "ROOM" && it.propertyId == property }.associate { it.id to it.title }, { room = it })
                        }
                    }
                    if (kind in setOf(Kind.TASK, Kind.REPAIR, Kind.APPLIANCE, Kind.EXPENSE)) {
                        Text(c.t("category")); Choice(category, categories.associateWith(c::t), { category = it })
                    }
                    if (kind in setOf(Kind.TASK, Kind.REPAIR)) {
                        Text(c.t("priority")); Choice(priority, listOf("low", "normal", "high", "urgent").associateWith(c::t), { priority = it })
                    }
                    if (kind in setOf(Kind.TASK, Kind.REPAIR, Kind.APPLIANCE, Kind.WARRANTY, Kind.APPOINTMENT)) EditField(if (kind in setOf(Kind.WARRANTY, Kind.APPLIANCE)) "expiry_date" else "due_date", due, { due = it }, "date")
                    if (kind in setOf(Kind.EXPENSE, Kind.REPAIR)) EditField("date", date, { date = it }, "date", true)
                    if (kind == Kind.TASK) {
                        Text(c.t("recurrence")); Choice(repeat, Repeat.entries.associate { it.name to c.t(it.name.lowercase()) }, { repeat = it })
                        if (repeat == "CUSTOM_DAYS") EditField("interval_days", interval, { interval = it }, "number", true)
                        Row(verticalAlignment = Alignment.CenterVertically) { Switch(paused, { paused = it }); Text(c.t("pause_recurrence"), Modifier.padding(start = 8.dp)) }
                    }
                    if (kind == Kind.REPAIR) { Text(c.t("status")); Choice(status, RepairStatus.entries.associate { it.name to c.t(it.name.lowercase()) }, { status = it }) }
                    if (kind in setOf(Kind.REPAIR, Kind.EXPENSE, Kind.APPOINTMENT)) {
                        Text(c.t("contractor")); Choice(contractor, mapOf("" to c.t("none")) + all.filter { it.kind == "CONTRACTOR" && it.propertyId == property }.associate { it.id to it.title }, { contractor = it })
                    }
                    if (kind in setOf(Kind.WARRANTY, Kind.EXPENSE, Kind.APPOINTMENT)) {
                        Text(c.t("related_record")); Choice(related, mapOf("" to c.t("none")) + all.filter { it.kind in setOf("REPAIR", "APPLIANCE", "TASK") && it.propertyId == property }.associate { it.id to it.title }, { related = it })
                    }
                    if (kind in setOf(Kind.EXPENSE, Kind.REPAIR, Kind.APPLIANCE)) {
                        EditField("currency", currency, { currency = it.uppercase().take(3) })
                        if (kind == Kind.EXPENSE) EditField("amount", amount, { amount = it }, "money", true)
                    }
                    extraFields[kind].orEmpty().forEach { f ->
                        if (f.choices.isNotEmpty()) { Text(c.t(f.key)); Choice(extras[f.key] ?: f.choices.first(), f.choices.associateWith(c::t), { extras[f.key] = it }) }
                        else EditField(f.key, extras[f.key].orEmpty(), { extras[f.key] = it }, f.type)
                    }
                    EditField("notes", notes, { notes = it }, multiline = true)
                    if (error) Text(c.t("form_error"), color = MaterialTheme.colorScheme.error)
                    Text(c.t("attachments_after_save"), style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = dismiss) { Text(c.t("cancel")) }
                    Button(onClick = {
                        try {
                            require(title.isNotBlank() && title.length <= 200 && notes.length <= 10000)
                            if (kind != Kind.PROPERTY) require(property.isNotBlank())
                            java.util.Currency.getInstance(currency)
                            if (due.isNotBlank()) LocalDate.parse(due)
                            LocalDate.parse(date)
                            val n = interval.toInt(); require(n in 1..3650)
                            extraFields[kind].orEmpty().forEach { f ->
                                val value = extras[f.key].orEmpty()
                                if (value.isNotBlank()) when (f.type) {
                                    "date" -> LocalDate.parse(value)
                                    "money" -> extras[f.key] = Money.decimal(Money.parse(value, currency, inputLocale), currency).toPlainString()
                                    "time" -> java.time.LocalTime.parse(value)
                                    "number" -> require(value.toInt() in 1..1440)
                                }
                                if (f.choices.isNotEmpty() && value.isEmpty()) extras[f.key] = f.choices.first()
                            }
                            if (kind == Kind.WARRANTY && !extras["start_date"].isNullOrBlank() && due.isNotBlank()) require(LocalDate.parse(extras["start_date"]) <= LocalDate.parse(due))
                            val value = if (kind == Kind.EXPENSE) Money.parse(amount, currency, inputLocale) else initial.amountMinor
                            save(initial.copy(title = title.trim(), notes = notes, propertyId = property.ifBlank { null }, roomId = room.ifBlank { null }, contractorId = contractor.ifBlank { null }, relatedId = related.ifBlank { null }, due = due.ifBlank { null }, date = date, category = category, priority = priority, status = status, repeat = repeat, interval = n, paused = paused, currency = currency, amountMinor = value, fields = Gson().toJson(extras.toMap()), anchorDay = if (due != initial.due && due.isNotBlank()) LocalDate.parse(due).dayOfMonth else initial.anchorDay))
                        } catch (_: Exception) { error = true }
                    }) { Text(c.t("save")) }
                }
            }
        }
    }
}

@Composable fun EditField(key: String, value: String, change: (String) -> Unit, type: String = "text", required: Boolean = false, multiline: Boolean = false) {
    val c = LocalContext.current
    OutlinedTextField(value, { change(if (type in setOf("date", "time", "number")) normalizeDigits(it) else it) }, Modifier.fillMaxWidth(), label = { Text(c.t(key) + if (required) " *" else "") }, singleLine = !multiline,
        minLines = if (multiline) 3 else 1,
        supportingText = if (type == "date") ({ Text(c.t("date_format")) }) else if (type == "money") ({ Text(c.t("money_format")) }) else if (type == "time") ({ Text(c.t("time_format")) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = when(type) { "money" -> KeyboardType.Decimal; "number" -> KeyboardType.Number; "phone" -> KeyboardType.Phone; "email" -> KeyboardType.Email; "url" -> KeyboardType.Uri; else -> KeyboardType.Text }))
}

@Composable fun RecordDetail(vm: HomeViewModel, r: HomeRecord, all: List<HomeRecord>, locale: Locale, dismiss: () -> Unit, edit: () -> Unit, addRoom: () -> Unit, openRecord: (HomeRecord) -> Unit) {
    val c = LocalContext.current
    val history by remember(r.id) { vm.repo.dao.history(r.id) }.collectAsState(initial = emptyList())
    val attachments by remember(r.id) { vm.repo.dao.attachments(r.id) }.collectAsState(initial = emptyList())
    var deleting by remember { mutableStateOf(false) }
    var attachmentPurpose by rememberSaveable(r.id) { mutableStateOf("document") }
    val attach = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.action {
            c.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            var name = c.t("document")
            c.contentResolver.query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) name = cursor.getString(0).take(200) }
            vm.repo.dao.attach(Attachment(recordId = r.id, uri = it.toString(), name = c.t(attachmentPurpose) + " • " + name, mime = c.contentResolver.getType(it) ?: "application/octet-stream"))
        } }
    }
    Dialog(onDismissRequest = dismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).widthIn(max = 760.dp).fillMaxHeight(.94f), shape = MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Heading(r.title, c.t(r.kind.lowercase())) }; IconButton(dismiss) { Icon(Icons.Outlined.Close, c.t("close")) } } }
                item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(edit) { Text(c.t("edit")) }
                    if (r.kind == "TASK") {
                        if (!r.done) OutlinedButton({ vm.action { vm.repo.complete(r.id) } }) { Text(c.t("mark_complete")) }
                        OutlinedButton({ vm.action { vm.repo.undoCompletion(r.id) } }) { Text(c.t("undo_completion")) }
                        OutlinedButton({ vm.action { vm.repo.snooze(r.id) } }) { Text(c.t("snooze")) }
                    }
                    if (r.kind == "PROPERTY") { OutlinedButton(addRoom) { Text(c.t("add_room")) }; OutlinedButton({ vm.save(r.copy(archived = !r.archived)) {} }) { Text(c.t(if (r.archived) "unarchive" else "archive")) } }
                } }
                item { Text(listOfNotNull(r.due?.let { c.t("due_date") + ": " + formattedDate(it, locale) }, if (r.kind == "REPAIR") c.t(r.status.lowercase()) else null, if (r.kind == "TASK") c.t(r.repeat.lowercase()) else null, all.find { it.id == r.roomId }?.title, all.find { it.id == r.contractorId }?.title).joinToString(" • ")) }
                if (r.kind == "EXPENSE") item { Text(formattedMoney(r, locale), style = MaterialTheme.typography.headlineMedium) }
                if (r.notes.isNotBlank()) item { Text(r.notes) }
                fieldsOf(r).filterKeys { it != "snoozedUntil" }.forEach { (key, value) -> if (value.isNotBlank()) item { Heading(c.t(key), if (extraFields[Kind.valueOf(r.kind)]?.find { it.key == key }?.choices?.isNotEmpty() == true) c.t(value) else value) } }
                if (r.kind == "CONTRACTOR") item { Row {
                    fieldsOf(r)["phone"]?.takeIf { it.isNotBlank() }?.let { phone -> OutlinedButton({ openExternal(c, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))) { vm.message.value = "no_compatible_app" } }) { Text(c.t("call")) } }
                    fieldsOf(r)["email"]?.takeIf { it.isNotBlank() }?.let { email -> OutlinedButton({ openExternal(c, Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null))) { vm.message.value = "no_compatible_app" } }) { Text(c.t("email")) } }
                } }
                val linked = all.filter { it.propertyId == r.id || it.relatedId == r.id || it.contractorId == r.id || it.roomId == r.id }
                items(linked) { child -> RecordCard(child, all, locale) { openRecord(child) } }
                item { Heading(c.t("documents"), c.t("document_privacy")); Choice(attachmentPurpose, listOf("document", "before_photo", "after_photo", "manual", "receipt", "certificate").associateWith(c::t), { attachmentPurpose = it }); OutlinedButton({ attach.launch(arrayOf("application/pdf", "image/*", "text/plain")) }) { Icon(Icons.Outlined.AttachFile, null); Text(c.t("attach_document")) } }
                items(attachments) { a -> Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ vm.action { openAttachment(c, a) { vm.message.value = "document_unavailable" } } }, Modifier.weight(1f)) { Text(a.name) }
                    IconButton({ vm.action {
                        vm.repo.dao.detach(a.id)
                        if(vm.repo.dao.attachments().none { it.uri == a.uri }) runCatching { c.contentResolver.releasePersistableUriPermission(Uri.parse(a.uri),Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    } }) { Icon(Icons.Outlined.LinkOff, c.t("remove_attachment")) }
                } }
                item { Heading(c.t("history")) }
                items(history.take(50)) { event -> Text(c.t(event.action) + " • " + java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, locale).format(java.util.Date(event.at)), style = MaterialTheme.typography.bodySmall) }
                item { TextButton({ deleting = true }) { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error); Text(c.t("delete"), color = MaterialTheme.colorScheme.error) } }
            }
        }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text(c.t("delete_record")) }, text = { Text(c.t(if (r.kind == "PROPERTY") "delete_property_warning" else "delete_warning")) }, confirmButton = { TextButton({ vm.action { vm.repo.delete(r.id); dismiss() }; deleting = false }) { Text(c.t("delete")) } }, dismissButton = { TextButton({ deleting = false }) { Text(c.t("cancel")) } })
}

fun openExternal(c: android.content.Context, intent: Intent, failed: () -> Unit) { try { c.startActivity(intent) } catch (_: Exception) { failed() } }

/** Detect removed documents and revoked grants before handing a dead URI to another app. */
suspend fun openAttachment(c: android.content.Context, file: Attachment, failed: () -> Unit) {
    val accessible = withContext(Dispatchers.IO) { runCatching {
        c.contentResolver.openAssetFileDescriptor(Uri.parse(file.uri),"r")?.use { true } ?: false
    }.getOrDefault(false) }
    if(!accessible) { failed(); return }
    openExternal(c,Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(file.uri),file.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),failed)
}

@Composable fun DocumentsPanel(vm: HomeViewModel, all: List<HomeRecord>) {
    val c = LocalContext.current
    var files by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    LaunchedEffect(all) { files = vm.repo.dao.attachments() }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Heading(c.t("documents"), c.t("document_privacy")) }
        if (files.isEmpty()) item { EmptyState(c.t("no_documents"), c.t("attachments_after_save"), Icons.Outlined.FolderOpen) }
        items(files) { file -> OutlinedCard(onClick = { vm.action { openAttachment(c,file) { vm.message.value = "document_unavailable" } } }, Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text(file.name); Text(all.find { it.id == file.recordId }?.title.orEmpty(), style = MaterialTheme.typography.bodySmall) } } }
    }
}
