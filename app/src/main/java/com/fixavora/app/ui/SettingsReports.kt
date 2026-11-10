package com.fixavora.app.ui

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fixavora.app.backup.*
import com.fixavora.app.data.*
import com.fixavora.app.reports.Exports
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

@Composable fun SettingsPanel(vm: HomeViewModel, prefs: Preferences) {
    val c = LocalContext.current
    var deleting by remember { mutableStateOf(false) }
    var backupMode by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    val confirmRestore by vm.restoreUri.collectAsStateWithLifecycle()
    val premium by vm.billing.state.collectAsStateWithLifecycle()
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val pass = vm.backupSession.take() ?: charArrayOf()
        uri?.let { vm.action { try {
            val bytes = withContext(Dispatchers.IO) { EncryptedBackup.encode(BackupData(records = vm.repo.dao.all(), events = vm.repo.dao.events()), pass) }
            withContext(Dispatchers.IO) { c.contentResolver.openOutputStream(it)?.use { out -> out.write(bytes) } ?: error("output") }
            vm.message.value = "backup_saved"
        } finally { pass.fill('\u0000') } } } ?: pass.fill('\u0000')
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.restoreUri.value = uri else vm.backupSession.clear() }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> vm.action { vm.settings.flag("reminders", granted) }; if (!granted) vm.message.value = "notifications_denied" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Heading(c.t("language"), c.t("translation_review_notice"))
        Choice(prefs.language, languages.mapValues { if (it.key.isEmpty()) c.t("system_language") else it.value }, { vm.action { vm.settings.text("language", it) } })
        Heading(c.t("currency")); Choice(prefs.currency, currencyChoices, { vm.action { vm.settings.text("currency", it) } })
        Heading(c.t("measurement_units")); Choice(prefs.units, listOf("metric", "imperial").associateWith(c::t), { vm.action { vm.settings.text("units", it) } })
        Toggle(c.t("dark_mode"), prefs.dark) { vm.action { vm.settings.flag("dark", it) } }
        Toggle(c.t("high_contrast"), prefs.contrast) { vm.action { vm.settings.flag("contrast", it) } }
        Toggle(c.t("spoken_feedback"), prefs.spoken) { vm.action { vm.settings.flag("spoken", it) } }
        Toggle(c.t("reminders"), prefs.reminders) {
            if (it && Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            else vm.action { vm.settings.flag("reminders", it) }
        }
        Text(c.t("reminder_limitations"), style = MaterialTheme.typography.bodySmall)
        Heading(c.t("quiet_hours"))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Choice(prefs.quietStart.toString(), (0..23).associate { it.toString() to "%02d:00".format(it) }, { vm.action { vm.settings.number("quietStart", it.toInt()) } })
            Choice(prefs.quietEnd.toString(), (0..23).associate { it.toString() to "%02d:00".format(it) }, { vm.action { vm.settings.number("quietEnd", it.toInt()) } })
        }
        Heading(c.t("privacy"), c.t("privacy_detail"))
        Text(c.t("backup_attachment_notice"))
        OutlinedButton({ if (premium.premium) backupMode = "backup" else vm.message.value = "premium_required" }) { Text(c.t("encrypted_backup")) }
        OutlinedButton({ backupMode = "restore" }) { Text(c.t("restore_backup")) }
        TextButton({ deleting = true }) { Text(c.t("delete_all_data"), color = MaterialTheme.colorScheme.error) }
        Text(c.t("safety_guardrail"), style = MaterialTheme.typography.bodySmall)
        Text("Fixavora ${com.fixavora.app.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall)
    }
    if (backupMode != null) AlertDialog(onDismissRequest = { backupMode = null; password = ""; vm.backupSession.clear() }, title = { Text(c.t(if (backupMode == "backup") "encrypted_backup" else "restore_backup")) }, text = { Column { Text(c.t("backup_password_notice")); OutlinedTextField(password, { password = it }, label = { Text(c.t("passphrase")) }, visualTransformation = PasswordVisualTransformation(), singleLine = true) } }, confirmButton = { TextButton({ val mode = backupMode; vm.backupSession.begin(password); password = ""; backupMode = null; if (mode == "backup") backup.launch("Fixavora-${LocalDate.now()}.fixavora") else restore.launch(arrayOf("application/octet-stream", "*/*")) }, enabled = password.length >= 12) { Text(c.t("continue_action")) } }, dismissButton = { TextButton({ backupMode = null; password = ""; vm.backupSession.clear() }) { Text(c.t("cancel")) } })
    confirmRestore?.let { uri -> AlertDialog(onDismissRequest = { vm.restoreUri.value = null; vm.backupSession.clear() }, title = { Text(c.t("restore_backup")) }, text = { Text(c.t("restore_warning")) }, confirmButton = { TextButton({
        val pass = vm.backupSession.take() ?: charArrayOf(); vm.restoreUri.value = null
        vm.action { try {
            val data = withContext(Dispatchers.IO) {
                val bytes = c.contentResolver.openInputStream(uri)?.use { source ->
                    val buffer = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    var count = source.read(chunk)
                    while (count >= 0) {
                        require(buffer.size() + count <= 20 * 1024 * 1024 + 100)
                        buffer.write(chunk, 0, count)
                        count = source.read(chunk)
                    }
                    buffer.toByteArray()
                } ?: error("input")
                EncryptedBackup.decode(bytes, pass)
            }
            EncryptedBackup.restore(vm.repo, data); vm.message.value = "restored"
        } finally { pass.fill('\u0000') } }
    }) { Text(c.t("restore_backup")) } }, dismissButton = { TextButton({ vm.restoreUri.value = null; vm.backupSession.clear() }) { Text(c.t("cancel")) } }) }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text(c.t("delete_all_data")) }, text = { Text(c.t("delete_all_warning")) }, confirmButton = { TextButton({ vm.action {
        vm.repo.dao.clear(); vm.settings.clear()
        vm.reportSnapshot = emptyList(); vm.reportRange = ""
        vm.backupSession.clear(); vm.restoreUri.value = null
        c.getSharedPreferences("notification_history", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        c.getSystemService(NotificationManager::class.java).cancelAll()
        c.contentResolver.persistedUriPermissions.forEach { runCatching { c.contentResolver.releasePersistableUriPermission(it.uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        deleting = false
    } }) { Text(c.t("delete")) } }, dismissButton = { TextButton({ deleting = false }) { Text(c.t("cancel")) } })
}

@Composable private fun Toggle(label: String, value: Boolean, change: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label, Modifier.weight(1f)); Switch(value, change) } }

@Composable fun PremiumPanel(vm: HomeViewModel) {
    val c = LocalContext.current
    val state by vm.billing.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HomeArt(Modifier.fillMaxWidth().height(140.dp), MaterialTheme.colorScheme.primary)
        Heading("Fixavora Premium", c.t("premium_intro"))
        listOf("premium_properties", "premium_records", "premium_reports", "premium_backup").forEach { key -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary); Text(c.t(key)) } }
        Text(c.t("one_time_purchase"), style = MaterialTheme.typography.titleLarge)
        Text(c.t("free_tier_detail"))
        state.price?.let { Text(it, style = MaterialTheme.typography.headlineLarge) }
        Text(c.t(state.message))
        Button({ vm.billing.buy() }, enabled = state.accountReady && state.price != null && !state.busy && !state.premium) { Text(c.t(if (state.premium) "premium_active" else "get_premium")) }
        OutlinedButton({ vm.billing.restore() }, enabled = !state.busy) { Text(c.t("restore_purchases")) }
        Text(c.t("premium_ai_policy"), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable fun ReportsPanel(vm: HomeViewModel, records: List<HomeRecord>, locale: Locale, premium: Boolean) {
    val c = LocalContext.current
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("all") }
    var export by remember { mutableStateOf<String?>(null) }
    var snapshot by remember { mutableStateOf(vm.reportSnapshot) }
    var rangeSnapshot by remember { mutableStateOf(vm.reportRange) }
    val filtered = records.filter { (type == "all" || it.kind == type) && (from.isEmpty() || (it.due ?: it.date) >= from) && (to.isEmpty() || (it.due ?: it.date) <= to) }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let { vm.action { withContext(Dispatchers.IO) { c.contentResolver.openOutputStream(it)?.use { out -> Exports.pdf(Exports.summary(c, vm.reportSnapshot, locale, vm.reportRange), out, locale) } ?: error("output") }; vm.message.value = "export_saved" } } }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { vm.action { withContext(Dispatchers.IO) { c.contentResolver.openOutputStream(it)?.use { out -> Exports.csv(c, vm.reportSnapshot, out) } ?: error("output") }; vm.message.value = "export_saved" } } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Heading(c.t("maintenance_history"), c.t("reports_intro"))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Column(Modifier.weight(1f)) { EditField("from_date", from, { from = it }, "date") }; Column(Modifier.weight(1f)) { EditField("to_date", to, { to = it }, "date") } }
        Choice(type, mapOf("all" to c.t("all")) + com.fixavora.app.domain.Kind.entries.associate { it.name to c.t(it.name.lowercase()) }, { type = it })
        ExpenseSummaryForReports(filtered, locale)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("pdf", "csv", "share_text").forEach { format -> OutlinedButton({
                if (!premium) vm.message.value = "premium_required"
                else try { if (from.isNotBlank()) LocalDate.parse(from); if (to.isNotBlank()) LocalDate.parse(to); require(from.isBlank() || to.isBlank() || from <= to); snapshot = filtered.toList(); rangeSnapshot = if (from.isBlank() && to.isBlank()) c.t("all_dates") else "$from — $to"; export = format } catch (_: Exception) { vm.message.value = "form_error" }
            }) { Text(if (format == "pdf" || format == "csv") format.uppercase() else c.t(format)) } }
        }
        Text(c.t("preview"), style = MaterialTheme.typography.titleLarge)
        Exports.summary(c, filtered, locale, if (from.isBlank() && to.isBlank()) c.t("all_dates") else "$from — $to").forEach { Text(it) }
    }
    export?.let { format -> AlertDialog(onDismissRequest = { export = null }, title = { Text(c.t("export_records")) }, text = { Text(c.t("export_privacy_warning")) }, confirmButton = { TextButton({
        export = null
        vm.reportSnapshot = snapshot.toList(); vm.reportRange = rangeSnapshot
        when(format) {
            "pdf" -> pdf.launch("Fixavora-${LocalDate.now()}.pdf")
            "csv" -> csv.launch("Fixavora-${LocalDate.now()}.csv")
            else -> openExternal(c, android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, Exports.summary(c, snapshot, locale, rangeSnapshot).joinToString("\n")), c.t("share_text"))) { vm.message.value = "no_compatible_app" }
        }
    }) { Text(c.t("continue_action")) } }, dismissButton = { TextButton({ export = null }) { Text(c.t("cancel")) } }) }
}

@Composable private fun ExpenseSummaryForReports(records: List<HomeRecord>, locale: Locale) {
    val c = LocalContext.current
    Text(c.t("currencies_kept_separate"), style = MaterialTheme.typography.bodySmall)
    records.filter { it.kind == "EXPENSE" }.groupBy { it.currency }.forEach { (code, rows) ->
        val total = rows.fold(java.math.BigInteger.ZERO) { sum, r -> sum + java.math.BigInteger.valueOf(r.amountMinor) }
        val format = java.text.NumberFormat.getCurrencyInstance(locale).apply { currency = java.util.Currency.getInstance(code) }
        Text(code + " • " + format.format(java.math.BigDecimal(total, java.util.Currency.getInstance(code).defaultFractionDigits)), style = MaterialTheme.typography.titleLarge)
    }
}
