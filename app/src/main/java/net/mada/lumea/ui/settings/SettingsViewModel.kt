package net.mada.lumea.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.backup.BackupManager
import net.mada.lumea.backup.Backup
import net.mada.lumea.backup.BackupSelection
import net.mada.lumea.backup.ReadOutcome
import net.mada.lumea.backup.RestoreMode
import net.mada.lumea.data.prefs.AssistantTone
import net.mada.lumea.data.prefs.CornerStyle
import net.mada.lumea.data.prefs.Palette
import net.mada.lumea.data.prefs.Settings
import net.mada.lumea.data.prefs.SettingsRepository
import net.mada.lumea.data.prefs.TextScale
import net.mada.lumea.data.prefs.ThemeMode
import net.mada.lumea.data.security.LockManager
import net.mada.lumea.notif.ReminderScheduler

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val lock: LockManager,
    private val backup: BackupManager,
) : ViewModel() {

    val settings: StateFlow<Settings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    fun isPinSet() = lock.isPinSet()

    fun setDisplayName(value: String) = viewModelScope.launch { repo.setDisplayName(value) }
    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { repo.setThemeMode(value) }
    fun setPalette(value: Palette) = viewModelScope.launch { repo.setPalette(value) }
    fun setDynamicColor(value: Boolean) = viewModelScope.launch { repo.setDynamicColor(value) }
    fun setTextScale(value: TextScale) = viewModelScope.launch { repo.setTextScale(value) }
    fun setCornerStyle(value: CornerStyle) = viewModelScope.launch { repo.setCornerStyle(value) }
    fun setHighContrast(value: Boolean) = viewModelScope.launch { repo.setHighContrast(value) }
    fun setShowGlow(value: Boolean) = viewModelScope.launch { repo.setShowGlow(value) }
    fun setAnimationsEnabled(value: Boolean) = viewModelScope.launch { repo.setAnimationsEnabled(value) }
    fun setCycleLength(value: Int) = viewModelScope.launch { repo.setCycleLength(value) }
    fun setPeriodLength(value: Int) = viewModelScope.launch { repo.setPeriodLength(value) }
    fun setLutealLength(value: Int) = viewModelScope.launch { repo.setLutealLength(value) }
    fun setCycleTabVisible(value: Boolean) = viewModelScope.launch { repo.setCycleTabVisible(value) }
    fun setPeriodReminder(value: Boolean) = viewModelScope.launch { repo.setPeriodReminder(value) }
    fun setPeriodReminderDaysBefore(value: Int) = viewModelScope.launch { repo.setPeriodReminderDaysBefore(value) }
    fun setFertileReminder(value: Boolean) = viewModelScope.launch { repo.setFertileReminder(value) }
    fun setHideFromRecents(value: Boolean) = viewModelScope.launch { repo.setHideFromRecents(value) }
    fun setBiometricEnabled(value: Boolean) = viewModelScope.launch { repo.setBiometricEnabled(value) }

    fun setAssistantName(value: String) = viewModelScope.launch { repo.setAssistantName(value) }
    fun setUserAlias(value: String) = viewModelScope.launch { repo.setUserAlias(value) }
    fun setOccupation(value: String) = viewModelScope.launch { repo.setOccupation(value) }
    fun setInterests(value: String) = viewModelScope.launch { repo.setInterests(value) }
    fun setAssistantTone(value: AssistantTone) = viewModelScope.launch { repo.setAssistantTone(value) }
    fun setAssistantNotes(value: String) = viewModelScope.launch { repo.setAssistantNotes(value) }
    fun setWidgetDiscreet(value: Boolean) = viewModelScope.launch { repo.setWidgetDiscreet(value) }

    fun setJournalReminder(context: Context, enabled: Boolean, minuteOfDay: Int) = viewModelScope.launch {
        repo.setJournalReminder(enabled)
        repo.setJournalReminderMinute(minuteOfDay)
        ReminderScheduler.scheduleJournalReminder(context, minuteOfDay.takeIf { enabled })
    }

    fun setPin(pin: String) = viewModelScope.launch {
        lock.setPin(pin)
        repo.setLockEnabled(true)
        _message.value = "Code enregistré"
    }

    fun removePin() = viewModelScope.launch {
        lock.clearPin()
        repo.setLockEnabled(false)
        repo.setBiometricEnabled(false)
        _message.value = "Code supprimé"
    }

    /** Fichier en attente d'une phrase de passe pour être lu. */
    private val _pendingImport = MutableStateFlow<Uri?>(null)
    val pendingImport: StateFlow<Uri?> = _pendingImport.asStateFlow()

    /**
     * Sauvegarde lue et déchiffrée, en attente du choix de l'utilisatrice.
     *
     * Tant qu'elle est ici, rien n'a été écrit dans la base : c'est le point où
     * l'écran affiche ce que contient le fichier et demande confirmation.
     */
    private val _preview = MutableStateFlow<Backup?>(null)
    val preview: StateFlow<Backup?> = _preview.asStateFlow()

    private val _restoring = MutableStateFlow(false)
    val restoring: StateFlow<Boolean> = _restoring.asStateFlow()

    fun clearPendingImport() { _pendingImport.value = null }
    fun clearPreview() { _preview.value = null }

    fun export(uri: Uri, passphrase: CharArray?) = viewModelScope.launch {
        backup.export(uri, passphrase).fold(
            onSuccess = {
                _message.value = if (passphrase == null) {
                    "Exporté en clair ($it éléments)"
                } else {
                    "Sauvegarde chiffrée exportée ($it éléments)"
                }
            },
            onFailure = { _message.value = "Échec de l'export : ${it.message}" },
        )
    }

    /**
     * Ouvre le fichier et s'arrête là : plus aucune donnée n'est écrasée à ce stade.
     * S'il est chiffré, la phrase de passe est réclamée avant même la lecture.
     */
    fun openBackup(uri: Uri, passphrase: CharArray? = null) = viewModelScope.launch {
        when (val outcome = backup.read(uri, passphrase)) {
            is ReadOutcome.Success -> {
                _pendingImport.value = null
                _preview.value = outcome.backup
            }
            ReadOutcome.PassphraseRequired -> _pendingImport.value = uri
            ReadOutcome.WrongPassphrase ->
                _message.value = "Phrase de passe incorrecte, ou fichier abîmé"
            is ReadOutcome.Failure -> {
                _pendingImport.value = null
                _message.value = "Échec de la lecture : ${outcome.message}"
            }
        }
    }

    /** Le seul point de l'app qui écrit réellement une sauvegarde dans la base. */
    fun restore(selection: BackupSelection, mode: RestoreMode) = viewModelScope.launch {
        val payload = _preview.value ?: return@launch
        _restoring.value = true
        backup.restore(payload, selection, mode).fold(
            onSuccess = {
                _message.value = when (mode) {
                    RestoreMode.REPLACE -> "Sauvegarde restaurée ($it éléments)"
                    RestoreMode.MERGE -> "Sauvegarde fusionnée ($it éléments)"
                }
            },
            onFailure = { _message.value = "Échec de la restauration : ${it.message}" },
        )
        _restoring.value = false
        _preview.value = null
    }

    fun suggestedFileName(encrypted: Boolean) = backup.suggestedFileName(encrypted)
}
