package net.mada.lumea.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.mada.lumea.data.db.DailyLogEntity
import net.mada.lumea.data.db.EVENT_SOURCE_CARE
import net.mada.lumea.data.db.EventEntity
import net.mada.lumea.data.db.FolderEntity
import net.mada.lumea.data.db.HabitCheckEntity
import net.mada.lumea.data.db.HabitEntity
import net.mada.lumea.data.db.LumeaDatabase
import net.mada.lumea.data.db.NoteEntity
import net.mada.lumea.data.db.PeriodEntity
import net.mada.lumea.data.db.PregnancyEntity
import net.mada.lumea.data.db.PrenatalCareEntity
import kotlinx.coroutines.flow.first
import net.mada.lumea.data.prefs.AssistantTone
import net.mada.lumea.data.prefs.CornerStyle
import net.mada.lumea.data.prefs.Palette
import net.mada.lumea.data.prefs.Settings
import net.mada.lumea.data.prefs.SettingsRepository
import net.mada.lumea.data.prefs.TextScale
import net.mada.lumea.data.prefs.ThemeMode
import net.mada.lumea.data.security.BackupCrypto
import java.security.GeneralSecurityException
import java.time.LocalDate

/**
 * Export / import d'une sauvegarde JSON via le sélecteur de fichiers du système.
 *
 * Le fichier produit n'est PAS chiffré : c'est à l'utilisatrice de choisir où elle
 * le range. L'écran de réglages le dit explicitement.
 */
class BackupManager(
    private val context: Context,
    private val db: LumeaDatabase,
    private val settings: SettingsRepository? = null,
) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /**
     * Écrit une sauvegarde dans [uri]. Sans [passphrase], le fichier est du JSON
     * lisible par n'importe qui — l'écran de réglages prévient explicitement.
     */
    suspend fun export(uri: Uri, passphrase: CharArray? = null): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = snapshot()
                val json = json.encodeToString(Backup.serializer(), payload).toByteArray()
                val bytes = passphrase?.let { BackupCrypto.encrypt(json, it) } ?: json

                context.contentResolver.openOutputStream(uri, "wt")?.use { out -> out.write(bytes) }
                    ?: error("Impossible d'écrire dans ce fichier")
                payload.count()
            }.also { passphrase?.fill('\u0000') }
        }

    /**
     * Les réglages font partie de la sauvegarde.
     *
     * Ils vivent dans DataStore et non dans la base, et se retrouvaient donc
     * exclus de l'export : restaurer sur un nouveau téléphone rendait bien les
     * notes et le cycle, mais perdait le prénom, le thème, la longueur de cycle,
     * les rappels et le profil de l'assistant. Le code PIN, lui, reste dehors
     * volontairement : il protège l'appareil, pas le fichier.
     */
    private suspend fun snapshot() = Backup(
        settings = settings?.settings?.first()?.toDto(),
        notes = db.noteDao().getAll().map(NoteEntity::toDto),
        folders = db.folderDao().getAll().map(FolderEntity::toDto),
        events = db.eventDao().getAll().map(EventEntity::toDto),
        periods = db.periodDao().getAll().map(PeriodEntity::toDto),
        logs = db.dailyLogDao().getAll().map(DailyLogEntity::toDto),
        habits = db.habitDao().getAll().map(HabitEntity::toDto),
        habitChecks = db.habitDao().getAllChecks().map(HabitCheckEntity::toDto),
        pregnancies = db.pregnancyDao().getAll().map(PregnancyEntity::toDto),
        prenatalCare = db.prenatalCareDao().getAll().map(PrenatalCareEntity::toDto),
    )

    /** Sérialise la sauvegarde en mémoire : sert aux sauvegardes automatiques. */
    suspend fun serialize(passphrase: CharArray): ByteArray = withContext(Dispatchers.IO) {
        val json = json.encodeToString(Backup.serializer(), snapshot()).toByteArray()
        BackupCrypto.encrypt(json, passphrase)
    }

    /** Dit si le fichier demande une phrase de passe, avant même de tenter l'import. */
    suspend fun needsPassphrase(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BackupCrypto.isEncrypted(stream.readNBytes(16))
            } ?: false
        }.getOrDefault(false)
    }

    /**
     * Ouvre et déchiffre une sauvegarde **sans rien écrire** dans la base.
     *
     * C'est la moitié qui manquait : avant, choisir un fichier suffisait à écraser
     * toutes les données de l'app. On lit d'abord, on montre ce qu'il y a dedans,
     * et c'est l'utilisatrice qui décide ensuite quoi en faire.
     */
    suspend fun read(uri: Uri, passphrase: CharArray? = null): ReadOutcome =
        withContext(Dispatchers.IO) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext ReadOutcome.Failure("Impossible de lire ce fichier")

                val plain = when {
                    !BackupCrypto.isEncrypted(raw) -> raw
                    passphrase == null -> return@withContext ReadOutcome.PassphraseRequired
                    else -> try {
                        BackupCrypto.decrypt(raw, passphrase)
                    } catch (e: GeneralSecurityException) {
                        // GCM rejette aussi bien une mauvaise phrase qu'un fichier abîmé :
                        // on ne peut pas distinguer les deux, on le dit honnêtement.
                        return@withContext ReadOutcome.WrongPassphrase
                    }
                }

                ReadOutcome.Success(json.decodeFromString(Backup.serializer(), plain.decodeToString()))
            } catch (e: Exception) {
                ReadOutcome.Failure(e.message ?: "Sauvegarde illisible")
            } finally {
                passphrase?.fill('\u0000')
            }
        }

    /**
     * Écrit tout ou partie d'une sauvegarde déjà lue, et renvoie le nombre
     * d'éléments réellement restaurés.
     *
     * En [RestoreMode.REPLACE], seules les catégories cochées sont vidées : ne pas
     * cocher « Agenda » laisse l'agenda actuel intact au lieu de le perdre en même
     * temps que le reste. En [RestoreMode.MERGE], rien n'est vidé.
     *
     * Le tout dans une seule transaction : une restauration interrompue à mi-chemin
     * ne laisse pas la base à moitié écrasée.
     */
    suspend fun restore(
        payload: Backup,
        selection: BackupSelection = BackupSelection(),
        mode: RestoreMode = RestoreMode.REPLACE,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val replace = mode == RestoreMode.REPLACE
            db.withTransaction {
                if (selection.notes) {
                    if (replace) db.noteDao().clear()
                    // Les dossiers suivent les notes : une note rangée dans un dossier
                    // absent se retrouverait orpheline.
                    payload.folders.forEach { db.folderDao().upsert(it.toEntity()) }
                    db.noteDao().insertAll(payload.notes.map { it.toEntity() })
                }
                if (selection.events) {
                    if (replace) db.eventDao().clear()
                    db.eventDao().insertAll(payload.events.map { it.toEntity() })
                }
                if (selection.periods) {
                    if (replace) db.periodDao().clear()
                    db.periodDao().insertAll(payload.periods.map { it.toEntity() })
                }
                if (selection.logs) {
                    if (replace) db.dailyLogDao().clear()
                    db.dailyLogDao().insertAll(payload.logs.map { it.toEntity() })
                }
                if (selection.periods) {
                    // Le suivi de grossesse suit les données de cycle : il en découle.
                    if (replace) {
                        db.pregnancyDao().clear()
                        db.prenatalCareDao().clear()
                        /*
                         * Et avec lui, les rendez-vous qu'il avait posés dans
                         * l'agenda. Sans cette ligne, remplacer les données de
                         * cycle laissait des CPN et des vaccins orphelins — avec
                         * leurs rappels — pour un suivi qui n'existait plus.
                         */
                        db.eventDao().deleteBySource(EVENT_SOURCE_CARE)
                    }
                    db.pregnancyDao().insertAll(payload.pregnancies.map { it.toEntity() })
                    db.prenatalCareDao().insertAll(payload.prenatalCare.map { it.toEntity() })
                }
                if (selection.habits) {
                    if (replace) {
                        db.habitDao().clearChecks()
                        db.habitDao().clearHabits()
                    }
                    db.habitDao().insertAll(payload.habits.map { it.toEntity() })
                    db.habitDao().insertAllChecks(payload.habitChecks.map { it.toEntity() })
                }
            }

            /*
             * Les réglages sont écrits hors de la transaction : ils vivent dans
             * DataStore, pas dans la base, et rien ne les lie aux tables.
             */
            if (selection.settings) payload.settings?.let { applySettings(it) }
            payload.count(selection)
        }
    }

    /**
     * Efface les catégories demandées, en une seule transaction.
     *
     * Le cycle emporte la grossesse, le carnet **et** les rendez-vous que le
     * carnet avait posés dans l'agenda : c'est précisément ce qui restait
     * orphelin jusqu'ici. Les événements saisis à la main ne sont jamais touchés,
     * sauf à cocher « Agenda ».
     */
    suspend fun erase(selection: EraseSelection): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.withTransaction {
                if (selection.notes) db.noteDao().clear()
                if (selection.events) db.eventDao().clear()
                if (selection.journal) db.dailyLogDao().clear()
                if (selection.habits) {
                    db.habitDao().clearChecks()
                    db.habitDao().clearHabits()
                }
                if (selection.cycle) {
                    db.periodDao().clear()
                    db.pregnancyDao().clear()
                    db.prenatalCareDao().clear()
                    db.eventDao().deleteBySource(EVENT_SOURCE_CARE)
                }
            }
        }
    }

    /** Réapplique les réglages sauvegardés. Le code PIN n'en fait jamais partie. */
    private suspend fun applySettings(dto: SettingsDto) {
        val repo = settings ?: return
        repo.setDisplayName(dto.displayName)
        runCatching { repo.setThemeMode(ThemeMode.valueOf(dto.themeMode)) }
        runCatching { repo.setPalette(Palette.valueOf(dto.palette)) }
        repo.setDynamicColor(dto.dynamicColor)
        runCatching { repo.setTextScale(TextScale.valueOf(dto.textScale)) }
        runCatching { repo.setCornerStyle(CornerStyle.valueOf(dto.cornerStyle)) }
        repo.setHighContrast(dto.highContrast)
        repo.setShowGlow(dto.showGlow)
        repo.setAnimationsEnabled(dto.animationsEnabled)
        repo.setCycleLength(dto.cycleLength)
        repo.setPeriodLength(dto.periodLength)
        repo.setLutealLength(dto.lutealLength)
        repo.setCycleTabVisible(dto.cycleTabVisible)
        repo.setPeriodReminder(dto.periodReminder)
        repo.setPeriodReminderDaysBefore(dto.periodReminderDaysBefore)
        repo.setFertileReminder(dto.fertileReminder)
        repo.setJournalReminder(dto.journalReminder)
        repo.setJournalReminderMinute(dto.journalReminderMinute)
        repo.setHideFromRecents(dto.hideFromRecents)
        repo.setAssistantName(dto.assistantName)
        repo.setUserAlias(dto.userAlias)
        repo.setOccupation(dto.occupation)
        repo.setInterests(dto.interests)
        runCatching { repo.setAssistantTone(AssistantTone.valueOf(dto.assistantTone)) }
        repo.setAssistantNotes(dto.assistantNotes)
        repo.setWidgetDiscreet(dto.widgetDiscreet)
    }

    fun suggestedFileName(encrypted: Boolean): String =
        if (encrypted) "lumea-${LocalDate.now()}.lumea" else "lumea-${LocalDate.now()}.json"
}

/** Issue d'une lecture de fichier : l'appelant doit pouvoir réclamer une phrase de passe. */
sealed interface ReadOutcome {
    data class Success(val backup: Backup) : ReadOutcome
    data object PassphraseRequired : ReadOutcome
    data object WrongPassphrase : ReadOutcome
    data class Failure(val message: String) : ReadOutcome
}

/** Ce qu'on accepte de reprendre de la sauvegarde. Tout est coché par défaut. */
data class BackupSelection(
    val notes: Boolean = true,
    val events: Boolean = true,
    val periods: Boolean = true,
    val logs: Boolean = true,
    val habits: Boolean = true,
    val settings: Boolean = true,
) {
    val isEmpty: Boolean get() =
        !notes && !events && !periods && !logs && !habits && !settings
}

/** Ce qu'on accepte d'effacer. Rien n'est coché par défaut. */
data class EraseSelection(
    val notes: Boolean = false,
    val events: Boolean = false,
    val cycle: Boolean = false,
    val journal: Boolean = false,
    val habits: Boolean = false,
) {
    val isEmpty: Boolean get() = !notes && !events && !cycle && !journal && !habits
}

enum class RestoreMode {
    /** Les catégories choisies sont vidées puis remplacées par la sauvegarde. */
    REPLACE,

    /** La sauvegarde s'ajoute à l'existant ; à identifiant égal, elle gagne. */
    MERGE,
}

@Serializable
data class Backup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val notes: List<NoteDto> = emptyList(),
    val folders: List<FolderDto> = emptyList(),
    val events: List<EventDto> = emptyList(),
    val periods: List<PeriodDto> = emptyList(),
    val logs: List<DailyLogDto> = emptyList(),
    val habits: List<HabitDto> = emptyList(),
    val habitChecks: List<HabitCheckDto> = emptyList(),
    val pregnancies: List<PregnancyDto> = emptyList(),
    val prenatalCare: List<PrenatalCareDto> = emptyList(),
    val settings: SettingsDto? = null,
) {
    fun count() = notes.size + events.size + periods.size + logs.size

    /** Le même décompte, restreint aux catégories cochées. */
    fun count(selection: BackupSelection) =
        (if (selection.settings && settings != null) 1 else 0) +
            (if (selection.notes) notes.size else 0) +
            (if (selection.events) events.size else 0) +
            (if (selection.periods) periods.size else 0) +
            (if (selection.logs) logs.size else 0) +
            (if (selection.habits) habits.size else 0)
}

@Serializable
data class NoteDto(
    val id: Long, val title: String, val body: String, val folderId: Long?, val tags: String,
    val colorIndex: Int, val isPinned: Boolean, val isFavorite: Boolean, val isChecklist: Boolean,
    val isArchived: Boolean, val isLocked: Boolean = false, val linkedDate: Long?,
    val createdAt: Long, val updatedAt: Long,
)

@Serializable
data class FolderDto(val id: Long, val name: String, val emoji: String, val colorIndex: Int, val position: Int)

@Serializable
data class EventDto(
    val id: Long, val title: String, val notes: String, val location: String, val startAt: Long,
    val endAt: Long, val allDay: Boolean, val colorIndex: Int, val reminderMinutes: Int?,
    val repeat: String, val isDone: Boolean, val linkedNoteId: Long?,
)

@Serializable
data class PeriodDto(val id: Long, val startDate: Long, val endDate: Long?)

@Serializable
data class DailyLogDto(
    val date: Long, val flow: Int, val symptoms: String, val mood: Int, val energy: Int,
    val journal: String, val gratitude: String, val waterGlasses: Int, val sleepHours: Float,
    val isLocked: Boolean = false, val updatedAt: Long,
)

@Serializable
data class HabitDto(val id: Long, val name: String, val emoji: String, val colorIndex: Int, val isActive: Boolean, val position: Int)

@Serializable
data class HabitCheckDto(val habitId: Long, val date: Long)

@Serializable
data class PregnancyDto(
    val id: Long, val testedOn: Long, val result: String, val lastPeriodStart: Long?,
    val status: String, val endedOn: Long?, val note: String, val createdAt: Long,
    val caregiverName: String = "", val caregiverRole: String = "",
    val caregiverPhone: String = "", val facility: String = "",
    val riskFactors: String = "",
    val birthDate: Long? = null,
)

@Serializable
data class SettingsDto(
    val displayName: String = "",
    val themeMode: String = "SYSTEM",
    val palette: String = "ROSE",
    val dynamicColor: Boolean = false,
    val textScale: String = "NORMAL",
    val cornerStyle: String = "NET",
    val highContrast: Boolean = false,
    val showGlow: Boolean = true,
    val animationsEnabled: Boolean = true,
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val lutealLength: Int = 14,
    val cycleTabVisible: Boolean = true,
    val periodReminder: Boolean = true,
    val periodReminderDaysBefore: Int = 2,
    val fertileReminder: Boolean = false,
    val journalReminder: Boolean = false,
    val journalReminderMinute: Int = 21 * 60,
    val hideFromRecents: Boolean = false,
    val assistantName: String = "",
    val userAlias: String = "",
    val occupation: String = "",
    val interests: String = "",
    val assistantTone: String = "SIMPLE",
    val assistantNotes: String = "",
    val widgetDiscreet: Boolean = true,
)

@Serializable
data class PrenatalCareDto(
    val id: Long, val pregnancyId: Long, val code: String, val doneOn: Long, val note: String,
)

private fun NoteEntity.toDto() = NoteDto(
    id, title, body, folderId, tags, colorIndex, isPinned, isFavorite, isChecklist,
    isArchived, isLocked, linkedDate?.toEpochDay(), createdAt, updatedAt,
)

private fun NoteDto.toEntity() = NoteEntity(
    id, title, body, folderId, tags, colorIndex, isPinned, isFavorite, isChecklist,
    isArchived, isLocked, linkedDate?.let(LocalDate::ofEpochDay), createdAt, updatedAt,
)

private fun FolderEntity.toDto() = FolderDto(id, name, emoji, colorIndex, position)
private fun FolderDto.toEntity() = FolderEntity(id, name, emoji, colorIndex, position)

private fun EventEntity.toDto() = EventDto(
    id, title, notes, location, startAt, endAt, allDay, colorIndex, reminderMinutes, repeat, isDone, linkedNoteId,
)

private fun EventDto.toEntity() = EventEntity(
    id, title, notes, location, startAt, endAt, allDay, colorIndex, reminderMinutes, repeat, isDone, linkedNoteId,
)

private fun PeriodEntity.toDto() = PeriodDto(id, startDate.toEpochDay(), endDate?.toEpochDay())
private fun PeriodDto.toEntity() =
    PeriodEntity(id, LocalDate.ofEpochDay(startDate), endDate?.let(LocalDate::ofEpochDay))

private fun DailyLogEntity.toDto() = DailyLogDto(
    date.toEpochDay(), flow, symptoms, mood, energy, journal, gratitude, waterGlasses,
    sleepHours, isLocked, updatedAt,
)

private fun DailyLogDto.toEntity() = DailyLogEntity(
    LocalDate.ofEpochDay(date), flow, symptoms, mood, energy, journal, gratitude, waterGlasses,
    sleepHours, isLocked, updatedAt,
)

private fun HabitEntity.toDto() = HabitDto(id, name, emoji, colorIndex, isActive, position)
private fun HabitDto.toEntity() = HabitEntity(id, name, emoji, colorIndex, isActive, position)

private fun PregnancyEntity.toDto() = PregnancyDto(
    id, testedOn.toEpochDay(), result, lastPeriodStart?.toEpochDay(), status,
    endedOn?.toEpochDay(), note, createdAt, caregiverName, caregiverRole,
    caregiverPhone, facility, riskFactors, birthDate?.toEpochDay(),
)

private fun PregnancyDto.toEntity() = PregnancyEntity(
    id, LocalDate.ofEpochDay(testedOn), result, lastPeriodStart?.let(LocalDate::ofEpochDay),
    status, endedOn?.let(LocalDate::ofEpochDay), note, createdAt, caregiverName,
    caregiverRole, caregiverPhone, facility, riskFactors,
    birthDate?.let(LocalDate::ofEpochDay),
)

private fun Settings.toDto() = SettingsDto(
    displayName, themeMode.name, palette.name, dynamicColor, textScale.name,
    cornerStyle.name, highContrast, showGlow, animationsEnabled, cycleLength,
    periodLength, lutealLength, cycleTabVisible, periodReminder,
    periodReminderDaysBefore, fertileReminder, journalReminder,
    journalReminderMinute, hideFromRecents, assistantName, userAlias, occupation,
    interests, assistantTone.name, assistantNotes, widgetDiscreet,
)

private fun PrenatalCareEntity.toDto() =
    PrenatalCareDto(id, pregnancyId, code, doneOn.toEpochDay(), note)

private fun PrenatalCareDto.toEntity() =
    PrenatalCareEntity(id, pregnancyId, code, LocalDate.ofEpochDay(doneOn), note)

private fun HabitCheckEntity.toDto() = HabitCheckDto(habitId, date.toEpochDay())
private fun HabitCheckDto.toEntity() = HabitCheckEntity(habitId, LocalDate.ofEpochDay(date))
