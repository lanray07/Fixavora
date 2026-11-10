package com.fixavora.app.ui

import android.app.Application
import androidx.lifecycle.*
import com.fixavora.app.FixavoraApp
import com.fixavora.app.data.*
import com.fixavora.app.premium.AmazonBilling
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as FixavoraApp
    val repo = app.repository
    val settings = app.settings
    val records = repo.dao.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val preferences = settings.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Preferences())
    val billing = AmazonBilling(app)
    val backupSession = com.fixavora.app.backup.BackupSession()
    val restoreUri = MutableStateFlow<android.net.Uri?>(null)
    // Retain the exact reviewed export while Android's document picker owns the
    // foreground. Activity recreation must not produce an empty/different file.
    var reportSnapshot: List<HomeRecord> = emptyList()
    var reportRange: String = ""
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val loaded = MutableStateFlow(false)
    init { viewModelScope.launch { repo.dao.all(); loaded.value = true } }
    fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            try { block() } catch (e: Exception) { message.value = if (e is RuleViolation) e.key else "operation_failed" } finally { busy.value = false }
        }
    }
    fun save(r: HomeRecord, onSaved: () -> Unit) = action { repo.save(r, billing.state.value.premium); onSaved(); message.value = "saved" }
    override fun onCleared() { backupSession.clear(); billing.close() }
}
