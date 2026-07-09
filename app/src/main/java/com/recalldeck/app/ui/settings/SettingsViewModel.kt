package com.recalldeck.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recalldeck.app.data.backup.BackupFormatException
import com.recalldeck.app.data.backup.BackupManager
import com.recalldeck.app.data.backup.CsvExporter
import com.recalldeck.app.data.db.SubjectEntity
import com.recalldeck.app.data.repo.AppSettings
import com.recalldeck.app.data.repo.DeckRepository
import com.recalldeck.app.data.repo.SettingsRepository
import com.recalldeck.app.data.repo.ThemeMode
import com.recalldeck.app.notifications.ReminderScheduler
import com.recalldeck.app.ui.common.containerViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalTime

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val subjects: List<SubjectEntity> = emptyList(),
    /** One-shot status message for backup/export/restore results. */
    val message: String? = null,
)

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val deckRepo: DeckRepository,
    private val backupManager: BackupManager,
    private val csvExporter: CsvExporter,
    private val appContext: Context,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> =
        combine(settingsRepo.settings, deckRepo.observeSubjects(), message) { settings, subjects, msg ->
            SettingsUiState(settings = settings, subjects = subjects, message = msg)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setRetentionTarget(value: Double) = launchUpdate { settingsRepo.setRetentionTarget(value) }

    fun setNewPerDay(value: Int) = launchUpdate { settingsRepo.setNewPerDay(value) }

    fun setThemeMode(mode: ThemeMode) = launchUpdate { settingsRepo.setThemeMode(mode) }

    fun setAutoSuspendMastered(value: Boolean) =
        launchUpdate { settingsRepo.setAutoSuspendMastered(value) }

    fun setReminderEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepo.setReminderEnabled(value)
            val settings = uiState.value.settings
            if (value) {
                ReminderScheduler.schedule(
                    appContext,
                    LocalTime.of(settings.reminderHour, settings.reminderMinute),
                )
            } else {
                ReminderScheduler.cancel(appContext)
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepo.setReminderTime(hour, minute)
            if (uiState.value.settings.reminderEnabled) {
                ReminderScheduler.schedule(appContext, LocalTime.of(hour, minute))
            }
        }
    }

    /** Writes a full JSON backup to [openStream] (invoked on the IO dispatcher). */
    fun exportBackup(openStream: () -> OutputStream?) {
        viewModelScope.launch {
            val json = backupManager.exportToJson()
            val ok = withContext(Dispatchers.IO) {
                try {
                    openStream()?.use { it.write(json.toByteArray(Charsets.UTF_8)); true } ?: false
                } catch (e: Exception) {
                    false
                }
            }
            message.value = if (ok) "Backup exported." else "Couldn't write the backup file."
        }
    }

    /** Replace-all restore from a JSON backup read from [openStream]. */
    fun importBackup(openStream: () -> InputStream?) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                try {
                    openStream()?.use { it.readBytes().toString(Charsets.UTF_8) }
                } catch (e: Exception) {
                    null
                }
            }
            if (content == null) {
                message.value = "Couldn't read the backup file."
                return@launch
            }
            message.value = try {
                backupManager.restoreFromJson(content)
                "Backup restored."
            } catch (e: BackupFormatException) {
                e.message ?: "Not a valid RecallDeck backup file."
            }
        }
    }

    /** Exports one subject's cards as `question;answer` CSV to [openStream]. */
    fun exportSubjectCsv(subjectId: Long, openStream: () -> OutputStream?) {
        viewModelScope.launch {
            val csv = csvExporter.exportSubject(subjectId)
            val ok = withContext(Dispatchers.IO) {
                try {
                    openStream()?.use { it.write(csv.toByteArray(Charsets.UTF_8)); true } ?: false
                } catch (e: Exception) {
                    false
                }
            }
            message.value = if (ok) "CSV exported." else "Couldn't write the CSV file."
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    private fun launchUpdate(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory = containerViewModelFactory {
            SettingsViewModel(
                it.settingsRepository,
                it.deckRepository,
                it.backupManager,
                it.csvExporter,
                it.applicationContext,
            )
        }
    }
}
