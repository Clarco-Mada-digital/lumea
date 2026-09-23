package net.mada.lumea.di

import android.content.Context
import net.mada.lumea.LumeaApp
import net.mada.lumea.backup.AutoBackup
import net.mada.lumea.backup.BackupManager
import net.mada.lumea.data.db.LumeaDatabase
import net.mada.lumea.data.prefs.SettingsRepository
import net.mada.lumea.data.repo.CycleRepository
import net.mada.lumea.data.repo.PregnancyRepository
import net.mada.lumea.ui.advisor.AdvisorConversation
import net.mada.lumea.data.repo.EventRepository
import net.mada.lumea.data.repo.JournalRepository
import net.mada.lumea.data.repo.NoteRepository
import net.mada.lumea.data.security.LockManager

/**
 * Injection de dépendances à la main : l'app est petite, un conteneur paresseux
 * suffit et évite un processeur d'annotations de plus au build.
 */
class AppContainer(private val context: Context) {

    val db: LumeaDatabase by lazy { LumeaDatabase.build(context) }

    val notes: NoteRepository by lazy { NoteRepository(db.noteDao(), db.folderDao()) }
    val events: EventRepository by lazy { EventRepository(db.eventDao()) }
    val cycle: CycleRepository by lazy { CycleRepository(db.periodDao(), db.dailyLogDao()) }
    val pregnancy: PregnancyRepository by lazy { PregnancyRepository(db.pregnancyDao(), db.prenatalCareDao()) }

    /** En mémoire, jamais sur disque : voir [AdvisorConversation]. */
    val advisorConversation: AdvisorConversation by lazy { AdvisorConversation() }
    val journal: JournalRepository by lazy { JournalRepository(db.dailyLogDao(), db.habitDao()) }

    val settings: SettingsRepository by lazy { SettingsRepository(context) }
    val lock: LockManager by lazy { LockManager(context) }
    val media: net.mada.lumea.data.media.MediaStore by lazy {
        net.mada.lumea.data.media.MediaStore(context)
    }
    val backup: BackupManager by lazy { BackupManager(context, db, settings, media) }
    val autoBackup: AutoBackup by lazy { AutoBackup(context, backup) }
}

fun Context.container(): AppContainer = (applicationContext as LumeaApp).container
