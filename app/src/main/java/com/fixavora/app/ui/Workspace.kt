@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.fixavora.app.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fixavora.app.backup.*
import com.fixavora.app.data.*
import com.fixavora.app.domain.*
import com.fixavora.app.reports.Exports
import com.fixavora.app.voice.VoiceController
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.*
import java.time.format.*
import java.util.*

data class Section(val key: String, val icon: ImageVector, val kind: Kind? = null)
val sections = listOf(
    Section("overview", Icons.Outlined.SpaceDashboard), Section("calendar", Icons.Outlined.CalendarMonth),
    Section("properties", Icons.Outlined.Cottage, Kind.PROPERTY), Section("tasks", Icons.Outlined.TaskAlt, Kind.TASK),
    Section("repairs", Icons.Outlined.Handyman, Kind.REPAIR), Section("appliances", Icons.Outlined.Kitchen, Kind.APPLIANCE),
    Section("warranties", Icons.Outlined.VerifiedUser, Kind.WARRANTY), Section("contractors", Icons.Outlined.Badge, Kind.CONTRACTOR),
    Section("expenses", Icons.Outlined.AccountBalanceWallet, Kind.EXPENSE), Section("appointments", Icons.Outlined.Event, Kind.APPOINTMENT),
    Section("documents", Icons.Outlined.FolderOpen), Section("reports", Icons.Outlined.Assessment),
    Section("assistant", Icons.Outlined.MicNone), Section("settings", Icons.Outlined.Settings), Section("premium", Icons.Outlined.WorkspacePremium))

@Composable fun FixavoraRoot(vm: HomeViewModel) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val base = LocalContext.current
    val context = remember(base, prefs.language) { base.localized(prefs.language) }
    val locale = context.resources.configuration.locales[0]
    CompositionLocalProvider(LocalContext provides context, LocalLayoutDirection provides if (locale.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
        FixavoraTheme(prefs.dark, prefs.contrast) { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Workspace(vm, prefs, locale) } }
    }
}

@Composable private fun Workspace(vm: HomeViewModel, prefs: Preferences, locale: Locale) {
    val c = LocalContext.current
    val all by vm.records.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf("overview") }
    var propertyId by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<HomeRecord?>(null) }
    var detail by remember { mutableStateOf<HomeRecord?>(null) }
    val properties = all.filter { it.kind == "PROPERTY" && !it.archived }
    val selected = properties.find { it.id == propertyId } ?: properties.firstOrNull()
    val records = all.filter { it.propertyId == selected?.id || it.id == selected?.id }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snack.showSnackbar(c.t(it)); vm.message.value = null } }
    val premium by vm.billing.state.collectAsStateWithLifecycle()
    Scaffold(snackbarHost = { SnackbarHost(snack) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 840.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    Column(Modifier.width(208.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface).padding(16.dp).verticalScroll(rememberScrollState())) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 20.dp)) { Icon(Icons.Outlined.Cottage, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(29.dp)); Spacer(Modifier.width(8.dp)); Text("Fixavora", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                        Text(c.t("home_companion"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(24.dp))
                        sections.forEach { item ->
                            NavigationDrawerItem(label = { Text(c.t(item.key), style = MaterialTheme.typography.labelLarge) }, selected = section == item.key, onClick = { section = item.key }, icon = { Icon(item.icon, null, Modifier.size(21.dp)) }, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp))
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(if (wide) c.t("your_home_organised") else "Fixavora", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(c.t(section), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { section = "assistant" }) { Icon(Icons.Outlined.MicNone, c.t("assistant")) }
                        if (selected != null) Choice(selected!!.id, properties.associate { it.id to it.title }, { propertyId = it }, Modifier.widthIn(max = 230.dp))
                    }
                    if (!wide) LazyRow(modifier = Modifier.testTag("compact-navigation"), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sections) { item -> FilterChip(selected = section == item.key, onClick = { section = item.key }, label = { Text(c.t(item.key)) }, leadingIcon = { Icon(item.icon, null, Modifier.size(18.dp)) }) }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (!loaded) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else if (!prefs.onboarding) Onboarding(vm, prefs, all) { editing = newRecord(Kind.PROPERTY, null, prefs); section = "properties" }
                    else Box(Modifier.weight(1f).padding(horizontal = if (wide) 32.dp else 16.dp)) {
                        when (section) {
                            "overview" -> Dashboard(records, selected, locale, onAdd = { editing = newRecord(Kind.TASK, selected?.id, prefs) }, open = { detail = it }, navigate = { section = it })
                            "calendar" -> CalendarView(records, locale) { detail = it }
                            "assistant" -> AssistantPanel(vm, selected, records, locale, prefs) { editing = it }
                            "reports" -> ReportsPanel(vm, records, locale, premium.premium)
                            "documents" -> DocumentsPanel(vm, all)
                            "settings" -> SettingsPanel(vm, prefs)
                            "premium" -> PremiumPanel(vm)
                            else -> {
                                val kind = sections.find { it.key == section }?.kind ?: Kind.TASK
                                RecordList(if (kind == Kind.PROPERTY) all.filter { it.kind == "PROPERTY" } else records.filter { it.kind == kind.name }, all, locale,
                                    add = { if (kind != Kind.PROPERTY && selected == null) vm.message.value = "create_property_first" else editing = newRecord(kind, selected?.id, prefs) }, open = { detail = it })
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { r -> RecordEditor(r, all, { editing = null }, { vm.save(it) { editing = null } }) }
    detail?.let { original ->
        val current = all.find { it.id == original.id }
        if (current == null) detail = null
        else RecordDetail(vm, current, all, locale, dismiss = { detail = null }, edit = { editing = current; detail = null }, addRoom = { editing = newRecord(Kind.ROOM, current.id, prefs); detail = null }, openRecord = { detail = it })
    }
}

fun newRecord(kind: Kind, propertyId: String?, prefs: Preferences) = HomeRecord(kind = kind.name, title = "", propertyId = if (kind == Kind.PROPERTY) null else propertyId, currency = prefs.currency, anchorDay = LocalDate.now().dayOfMonth)

@Composable private fun Onboarding(vm: HomeViewModel, prefs: Preferences, all: List<HomeRecord>, create: () -> Unit) {
    val c = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HomeArt(Modifier.fillMaxWidth().height(150.dp), MaterialTheme.colorScheme.primary)
        Text(c.t("welcome"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(c.t("tagline"), style = MaterialTheme.typography.titleMedium)
        Text(c.t("privacy_intro"))
        Choice(prefs.language, languages.mapValues { if (it.key.isEmpty()) c.t("system_language") else it.value }, { vm.action { vm.settings.text("language", it) } })
        Choice(prefs.role, listOf("homeowner", "renter", "landlord").associateWith(c::t), { vm.action { vm.settings.text("role", it) } })
        Choice(prefs.currency, listOf("GBP", "USD", "EUR", "AUD", "CAD", "BRL", "JPY", "INR", "SAR", "TRY", "IDR", "PLN", "SEK").associateWith { it }, { vm.action { vm.settings.text("currency", it) } })
        Choice(prefs.units, listOf("metric", "imperial").associateWith(c::t), { vm.action { vm.settings.text("units", it) } })
        Button(onClick = { vm.action { vm.settings.flag("onboarding", true); create() } }, Modifier.heightIn(min = 52.dp)) { Text(c.t("create_first_property")) }
        TextButton(onClick = { vm.action { vm.settings.flag("onboarding", true) } }) { Text(c.t("skip_for_now")) }
    }
}

@Composable fun Choice(value: String, options: Map<String, String>, change: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, Modifier.heightIn(min = 48.dp)) { Text(options[value] ?: value, maxLines = 2); Icon(Icons.Outlined.ExpandMore, null) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            options.forEach { (key, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { change(key); expanded = false }) }
        }
    }
}

@Composable private fun Dashboard(records: List<HomeRecord>, property: HomeRecord?, locale: Locale, onAdd: () -> Unit, open: (HomeRecord) -> Unit, navigate: (String) -> Unit) {
    val c = LocalContext.current
    var window by rememberSaveable { mutableStateOf("week") }
    val today = LocalDate.now()
    val tasks = records.filter { it.kind == "TASK" && !it.done && !it.archived }
    val due = tasks.filter { it.due != null && when(window) { "today" -> it.due <= today.toString(); "week" -> it.due <= today.plusDays(7).toString(); "month" -> it.due <= today.plusMonths(1).toString(); else -> true } }.sortedBy { it.due }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            Surface(color = Navy, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(c.t("a_little_care"), color = Color(0xFFB7D8D7), style = MaterialTheme.typography.labelLarge)
                        Text(property?.title ?: c.t("make_room_for_calm"), color = Ivory, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(c.t("dashboard_intro"), color = Color(0xFFDCE6E8))
                        Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCE6E8), contentColor = Navy)) { Icon(Icons.Outlined.Add, null); Text(c.t("add_task")) }
                    }
                    HomeArt(Modifier.widthIn(max = 190.dp).width(150.dp).height(165.dp))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(c.t("due_soon"), tasks.count { it.due != null && it.due <= today.plusDays(7).toString() }.toString(), Modifier.weight(1f)) { navigate("tasks") }
                Stat(c.t("overdue"), tasks.count { it.due != null && it.due < today.toString() }.toString(), Modifier.weight(1f)) { navigate("tasks") }
                Stat(c.t("active_repairs"), records.count { it.kind == "REPAIR" && it.status !in setOf("COMPLETED", "CANCELLED") }.toString(), Modifier.weight(1f)) { navigate("repairs") }
            }
        }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("today", "week", "month", "all").forEach { key -> FilterChip(window == key, { window = key }, label = { Text(c.t(key)) }) } } }
        item { Heading(c.t("coming_up"), c.t("maintenance_at_your_pace")) }
        if (due.isEmpty()) item { EmptyState(c.t("nothing_due"), c.t("add_your_next_task"), Icons.Outlined.TaskAlt) }
        items(due.take(8), key = { it.id }) { RecordCard(it, records, locale) { open(it) } }
        item { Heading(c.t("home_records"), c.t("everything_in_one_place")) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(c.t("appliances"), records.count { it.kind == "APPLIANCE" }.toString(), Modifier.weight(1f)) { navigate("appliances") }
                Stat(c.t("warranty_alerts"), records.count { it.kind in setOf("WARRANTY", "APPLIANCE") && it.due != null && it.due >= today.toString() && it.due <= today.plusDays(30).toString() }.toString(), Modifier.weight(1f)) { navigate("warranties") }
            }
        }
        items(records.filter { it.kind == "APPOINTMENT" && it.due != null && it.due >= today.toString() }.take(3)) { RecordCard(it, records, locale) { open(it) } }
        item { ExpenseSummary(records.filter { it.kind == "EXPENSE" }, locale) }
        item { Heading(c.t("recent_activity"), c.t("saved_on_this_device")) }
        items(records.filter { it.kind !in setOf("PROPERTY", "ROOM") }.take(4)) { RecordCard(it, records, locale) { open(it) } }
        item { AssistChip(onClick = { navigate("assistant") }, label = { Text(c.t("ask_your_assistant")) }, leadingIcon = { Icon(Icons.Outlined.MicNone, null) }) }
    }
}

@Composable private fun Stat(label: String, value: String, modifier: Modifier, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(label, style = MaterialTheme.typography.labelLarge) }
    }
}
@Composable fun Heading(title: String, caption: String = "") { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); if (caption.isNotEmpty()) Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable fun EmptyState(title: String, caption: String, icon: ImageVector) { Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Heading(title, caption) } } }

fun formattedDate(value: String, locale: Locale): String = runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) }.getOrDefault(value)
fun formattedMoney(r: HomeRecord, locale: Locale): String = NumberFormat.getCurrencyInstance(locale).apply { currency = Currency.getInstance(r.currency) }.format(Money.decimal(r.amountMinor, r.currency))

@Composable fun RecordCard(r: HomeRecord, all: List<HomeRecord>, locale: Locale, click: () -> Unit) {
    val c = LocalContext.current
    val icon = sections.find { it.kind?.name == r.kind }?.icon ?: Icons.Outlined.MeetingRoom
    Surface(onClick = click, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Icon(icon, null, Modifier.padding(13.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(all.find { it.id == r.roomId }?.title, r.due?.let { formattedDate(it, locale) }, if (r.kind == "REPAIR") c.t(r.status.lowercase()) else null, if (r.kind == "EXPENSE") formattedMoney(r, locale) else null).joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (r.done) Icon(Icons.Outlined.CheckCircle, c.t("completed"), tint = MaterialTheme.colorScheme.primary)
            else if (r.due != null && r.due < LocalDate.now().toString()) Text(c.t("overdue"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            else Icon(Icons.Outlined.ChevronRight, null)
        }
    }
}

@Composable private fun RecordList(records: List<HomeRecord>, all: List<HomeRecord>, locale: Locale, add: () -> Unit, open: (HomeRecord) -> Unit) {
    val c = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var room by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("due_date") }
    val visible = records.filter { (it.title + it.notes).contains(query, true) && (room.isBlank() || it.roomId == room) && when (filter) { "active" -> !it.archived && !it.done && it.status !in setOf("COMPLETED", "CANCELLED"); "completed" -> it.done || it.status == "COMPLETED"; "archived" -> it.archived; else -> true } }.let { list -> when(sort) { "title" -> list.sortedBy { it.title }; "recent_activity" -> list.sortedByDescending { it.updatedAt }; else -> list.sortedBy { it.due ?: "9999" } } }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(query, { query = it }, Modifier.weight(1f), label = { Text(c.t("search_records")) }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true); Button(onClick = add, Modifier.heightIn(min = 52.dp)) { Icon(Icons.Outlined.Add, null); Text(c.t("add")) } } }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice(filter, listOf("all", "active", "completed", "archived").associateWith(c::t), { filter = it })
            Choice(sort, listOf("due_date", "title", "recent_activity").associateWith(c::t), { sort = it })
            Choice(room, mapOf("" to c.t("all_rooms")) + all.filter { it.kind == "ROOM" && records.any { r -> r.propertyId == it.propertyId } }.associate { it.id to it.title }, { room = it })
        } }
        item { Text(c.resources.getQuantityString(com.fixavora.app.R.plurals.record_count, visible.size, visible.size), style = MaterialTheme.typography.labelMedium) }
        if (visible.isEmpty()) item { EmptyState(c.t("no_records"), c.t("add_record_intro"), Icons.Outlined.Inventory2) }
        items(visible, key = { it.id }) { RecordCard(it, all, locale) { open(it) } }
    }
}

@Composable private fun CalendarView(records: List<HomeRecord>, locale: Locale, open: (HomeRecord) -> Unit) {
    val c = LocalContext.current
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val month = YearMonth.now().plusMonths(offset.toLong())
    val first = month.atDay(1)
    val weekStart = java.time.temporal.WeekFields.of(locale).firstDayOfWeek.value
    val padding = (first.dayOfWeek.value - weekStart + 7) % 7
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { IconButton({ offset-- }) { Icon(Icons.Outlined.ChevronLeft, c.t("previous_month")) }; Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)), style = MaterialTheme.typography.titleLarge); IconButton({ offset++ }) { Icon(Icons.Outlined.ChevronRight, c.t("next_month")) } } }
        item { Row { (0..6).forEach { day -> Text(DayOfWeek.of((weekStart + day - 1) % 7 + 1).getDisplayName(java.time.format.TextStyle.SHORT, locale), Modifier.weight(1f).padding(8.dp), style = MaterialTheme.typography.labelMedium) } } }
        items((padding + month.lengthOfMonth() + 6) / 7) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { (0..6).forEach { column ->
                val day = row * 7 + column - padding + 1
                if (day !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f)) else {
                    val date = month.atDay(day)
                    val entries = records.filter { it.due == date.toString() && !it.done }
                    Surface(onClick = { entries.firstOrNull()?.let(open) }, modifier = Modifier.weight(1f).heightIn(min = 72.dp), color = if (date == LocalDate.now()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp)) {
                        Column(Modifier.padding(10.dp)) { Text(NumberFormat.getIntegerInstance(locale).format(day)); if (entries.isNotEmpty()) Text(NumberFormat.getIntegerInstance(locale).format(entries.size) + " •", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            } }
        }
        item { Heading(c.t("scheduled_this_month")) }
        items(records.filter { it.due?.startsWith(month.toString()) == true }.sortedBy { it.due }) { RecordCard(it, records, locale) { open(it) } }
    }
}

@Composable private fun ExpenseSummary(records: List<HomeRecord>, locale: Locale) {
    val c = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Heading(c.t("spending_overview"), c.t("currencies_kept_separate"))
        records.groupBy { it.currency }.forEach { (currency, rows) ->
            val totals = rows.groupBy { it.category }.mapValues { (_, group) -> group.fold(java.math.BigInteger.ZERO) { sum, r -> sum + java.math.BigInteger.valueOf(r.amountMinor) } }
            val total = totals.values.fold(java.math.BigInteger.ZERO, java.math.BigInteger::add)
            val format = NumberFormat.getCurrencyInstance(locale).apply { this.currency = Currency.getInstance(currency) }
            Text(format.format(java.math.BigDecimal(total, Currency.getInstance(currency).defaultFractionDigits)), style = MaterialTheme.typography.titleLarge)
            totals.forEach { (category, value) ->
                Text(c.t(category) + " • " + format.format(java.math.BigDecimal(value, Currency.getInstance(currency).defaultFractionDigits)), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { if (total.signum() == 0) 0f else (value.toDouble() / total.toDouble()).toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp))
            }
        }
        if (records.isEmpty()) Text(c.t("no_expenses"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
